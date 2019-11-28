/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package BUEPA.RTIP.MQTT.StreamingJobs;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBConfig;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBSink;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.util.Collector;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StreamingJob {

    public static void main(String[] args) throws Exception {


        HashMap<String, String> conf = new HashMap<String, String>();

        conf.put("rmq-hostname", "rmq-cluster-rabbitmq-ha.mqtt.svc.cluster.local");
        conf.put("rmq-port", "5672");
        conf.put("rmq-username", "guest");
        conf.put("rmq-password", "Pa55w.rd");
        conf.put("rmq-vhost", "/");
        conf.put("rmq-queuename", "graz.sensors.mqtt.pm2.detection");

        conf.put("influx-hostname", "http://influxdb.influxdb:8086");
        conf.put("influx-username", "admin");
        conf.put("influx-password", "Pa55w.rd");
        conf.put("influx-db", "mqtt");

        conf.put("sensor-number", "10000");
        conf.put("sensor-areas", "16");
        conf.put("lat-long-range", "100");
        conf.put("percentage-sensors", "10");

        int sensorAreas = Integer.parseInt(conf.get("sensor-areas"));
        int sensors = Integer.parseInt(conf.get("sensor-number"));


        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        // Set up a configuration for the RabbitMQ Source
        final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
                .setHost(conf.get("rmq-hostname"))
                .setPort(Integer.parseInt(conf.get("rmq-port")))
                .setUserName(conf.get("rmq-username"))
                .setPassword(conf.get("rmq-password"))
                .setVirtualHost(conf.get("rmq-vhost"))
                .build();

        // Initiating a Data Stream from RabbitMQ
        final DataStream<String> RMQDS = env
                .addSource(new RMQSource<String>(
                        connectionConfig,                   // config for the RabbitMQ connection
                        conf.get("rmq-queuename"),          // name of the RabbitMQ queue to consume
                        false,               // use correlation ids; can be false if only at-least-once is required
                        new SimpleStringSchema()))          // deserialization schema to turn messages into Java objects
                .setParallelism(1);                         // non-parallel Source

        //Extraction of values of the RMQ-Data Stream
        final DataStream<Triplet<String, Double, String>> extractedDS = RMQDS.map(
                new RichMapFunction<String, Triplet<String, Double, String>>() {
                    @Override
                    public Triplet<String, Double, String> map(String s) throws Exception {
                        // Extract the payload of the message
                        String[] input = s.split(",");

                        // Extract the sensor ID
                        String sensorID = input[1];
                        String unformattedID = sensorID.split(":")[1];
                        String id = unformattedID.replaceAll(" ", "");

                        // Extract longitude
                        String sensorLONG = input[2];
                        String unformattedLONGTD = sensorLONG.split(":")[1];
                        String longtd = unformattedLONGTD.replaceAll(" ", "");
                        int lon = Integer.parseInt(longtd);

                        // Extract latitude
                        String sensorLAT = input[3];
                        String unformattedLATD = sensorLAT.split(":")[1];
                        String latd = unformattedLATD.replaceAll(" ", "");
                        int lat = Integer.parseInt(latd);

                        // Extract the particulate matter
                        String sensorPM2 = input[6];
                        String unformattedPM2 = sensorPM2.split(":")[1];
                        String pm2String = unformattedPM2.replaceAll("[ }]+", "");

                        //Extract the double value of PM2 String
                        double pm2 = Double.valueOf(pm2String).doubleValue();

                        //Initialize the needed values for area detection
                        String area = "";
                        //If you calculate the square root of the number of areas a integer should result
                        if ((Math.sqrt(sensorAreas)) % 1 == 0) {
                            area = "("
                                    + ((lat - 1) / (int) ((Integer.parseInt(conf.get("lat-long-range"))) / Math.sqrt(sensorAreas)))
                                    + ","
                                    + ((lon - 1) / (int) (Integer.parseInt(conf.get("lat-long-range")) / Math.sqrt(sensorAreas)))
                                    + ")";
                        } else {
                            throw new IllegalArgumentException("The square root of the number of areas must not have a remainder!");
                        }

                        Triplet<String, Double, String> sensorData = Triplet.with(id, pm2, area);
                        return sensorData;
                    }

                }
        );

        DataStream<Pair<Integer, String>> processedDS = extractedDS
                .keyBy(qt2 -> qt2.getValue0())
                .timeWindow(Time.seconds(60))
                .process(new DetectPM2RisePerSensor())
                .keyBy(p -> p.getValue1())
                .timeWindow(Time.seconds(15))
                .reduce(new ReduceFunction<Pair<Integer, String>>() {
                    public Pair<Integer, String> reduce(Pair<Integer, String> value1, Pair<Integer, String> value2) throws Exception {
                        return new Pair<Integer, String>(value1.getValue0() + value2.getValue0(), value1.getValue1());
                    }
                });

        processedDS
                .map((Pair<Integer, String> pair) -> (pair.getValue0() >
                        (sensors / sensorAreas / 10))
                        ? "Area: " + pair.getValue1() + " registered a too high increase of PM2,5 concentration:" + pair.getValue0() + " of "
                        + (sensors / sensorAreas) + " registered sensors reported."
                        : "Area: " + pair.getValue1() + " registered a normal PM2,5 concentration, num: " + pair.getValue0() + " of "
                        + (sensors / sensorAreas) + " registered sensors reported.")
                .print();

        final DataStream<InfluxDBPoint> influxDBSinkingDS = processedDS
                .map(valuePair -> {

                            // Create the timestamp
                            long timestamp = System.currentTimeMillis();

                            //Set the tags
                            HashMap<String, String> tags = new HashMap<>();
                            tags.put("area", valuePair.getValue1());

                            //Set the fields
                            HashMap<String, Object> fields = new HashMap<>();
                            fields.put("sensorsReportingTrend", valuePair.getValue0());
                            fields.put("sensorsPerArea", sensors / sensorAreas);
                            fields.put("criticalNumOfSensors", sensors / sensorAreas / 10);

                            return new InfluxDBPoint("AreaTrend", timestamp, tags, fields);

                        }
                );


        //Sinking the Data Stream to InfluxDB
        InfluxDBConfig influxDBConfig = InfluxDBConfig.builder(conf.get("influx-hostname"), conf.get("influx-username"), conf.get("influx-password"), conf.get("influx-db"))
                .batchActions(1000)
                .flushDuration(100, TimeUnit.MILLISECONDS)
                .enableGzip(true)
                .build();

        influxDBSinkingDS.addSink(new InfluxDBSink(influxDBConfig));

        // execute program
        env.execute("MQTT Detection StreamingJob");
    }

    public static class DetectPM2RisePerSensor
            extends ProcessWindowFunction<Triplet<String, Double, String>, Pair<Integer, String>, String, TimeWindow> {

        @Override
        public void process(String key, Context context, Iterable<Triplet<String, Double, String>> input, Collector<Pair<Integer, String>> out) throws IOException {
            List<Double> pm2Values = new ArrayList<Double>();

            String area = input.iterator().next().getValue2();
            input.iterator().forEachRemaining(q -> pm2Values.add(q.getValue1()));

            if (pm2Values.size() > 1) {

                Double boundVal = new Double(pm2Values.size() / 10);
                int boundaryValue = boundVal.intValue();


                List<Double> firstTenPercent = pm2Values.subList(0, 1 + boundaryValue);
                List<Double> lastTenPercent = pm2Values.subList(((pm2Values.size() - boundaryValue) - 1), pm2Values.size());

                double accFirst = 0d;
                for (Double dbl : firstTenPercent) {
                    accFirst = accFirst + dbl;
                }

                double accLast = 0d;
                for (Double dbl : lastTenPercent) {
                    accLast = accLast + dbl;
                }

                double difference = (accLast / lastTenPercent.size()) - (accFirst / firstTenPercent.size());

                if (difference > 10) {
                    out.collect(new Pair(1, area));
                } else {
                    out.collect(new Pair(0, area));
                }
            } else {
                out.collect(new Pair(0, area));
            }
        }
    }
}