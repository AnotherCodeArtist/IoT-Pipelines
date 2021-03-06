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

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBConfig;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBSink;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.javatuples.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StreamingJob {


    public static final OutputTag<Ennead<String, Boolean, String, Double, Integer, Double, Double, Double, Double>> sensorOutput
            = new OutputTag<Ennead<String, Boolean, String, Double, Integer, Double, Double, Double, Double>>("sensorOutput") {
    };

    public static void main(String[] args) throws Exception {

        HashMap<String, String> conf = new HashMap<String, String>();

        conf.put("rmq-hostname", "PUT YOUR OWN CONFIG");
        conf.put("rmq-port", "PUT YOUR OWN CONFIG");
        conf.put("rmq-username", "PUT YOUR OWN CONFIG");
        conf.put("rmq-password", "PUT YOUR OWN CONFIG");
        conf.put("rmq-vhost", "PUT YOUR OWN CONFIG");
        conf.put("rmq-queuename", "PUT YOUR OWN CONFIG");
        conf.put("influx-hostname", "PUT YOUR OWN CONFIG");
        conf.put("influx-username", "PUT YOUR OWN CONFIG");
        conf.put("influx-password", "PUT YOUR OWN CONFIG");
        conf.put("influx-db", "PUT YOUR OWN CONFIG");

        conf.put("sensor-number", "10000");
        conf.put("sensor-areas", "16");
        conf.put("lat-long-range", "100");
        conf.put("percentage-sensors", "10");

        int sensorAreas = Integer.parseInt(conf.get("sensor-areas"));

        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        //Sinking the Data Stream to InfluxDB
        InfluxDBConfig influxDBConfig = InfluxDBConfig.builder(conf.get("influx-hostname"), conf.get("influx-username"), conf.get("influx-password"), conf.get("influx-db"))
                .batchActions(1000)
                .flushDuration(100, TimeUnit.MILLISECONDS)
                .enableGzip(true)
                .build();

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
                .setParallelism(1);// non-parallel Source

        //Extraction of values of the RMQ-Data Stream
        final DataStream<Septet<String, Double, String, String, String, Double, Double>> extractedDS = RMQDS.map(
                new RichMapFunction<String, Septet<String, Double, String, String, String, Double, Double>>() {
                    @Override
                    public Septet<String, Double, String, String, String, Double, Double> map(String s) throws Exception {
                        // Extract the payload of the message
                        String[] input = s.split(",");

                        // Extract the sensor ID
                        String sensorID = input[1];
                        String unformattedID = sensorID.split(":")[1];
                        String id = unformattedID.replaceAll(" ", "");

                        // Extract Temperature
                        String sensorLONG = input[2];
                        String unformattedLONGTD = sensorLONG.split(":")[1];
                        String longString = unformattedLONGTD.replaceAll(" ", "");
                        int lon = Integer.parseInt(longString);

                        // Extract latitude
                        String sensorLAT = input[3];
                        String unformattedLATD = sensorLAT.split(":")[1];
                        String latString = unformattedLATD.replaceAll(" ", "");
                        int lat = Integer.parseInt(latString);

                        // Extract the humidity
                        String sensorHUM = input[4];
                        String unformattedHUM = sensorHUM.split(":")[1];
                        String hum = unformattedHUM.replaceAll(" ", "");
                        double humid = Double.valueOf(hum).doubleValue();

                        // Extract the temperature
                        String sensorTEMP = input[5];
                        String unformattedTEMP = sensorTEMP.split(":")[1];
                        String temp = unformattedTEMP.replaceAll(" ", "");
                        double tempC = Double.valueOf(temp).doubleValue();

                        // Extract the particulate matter
                        String sensorPM2 = input[6];
                        String unformattedPM2 = sensorPM2.split(":")[1];
                        String pm2String = unformattedPM2.replaceAll("[ }]+", "");
                        double pm2 = Double.valueOf(pm2String).doubleValue();

                        //Initialize the needed values for area detection
                        String area = "";
                        //If you calculate the square root of the number of areas an integer should result
                        if ((Math.sqrt(sensorAreas)) % 1 == 0) {
                            area = "("
                                    + ((lat - 1) / (int) ((Integer.parseInt(conf.get("lat-long-range"))) / Math.sqrt(sensorAreas)))
                                    + ","
                                    + ((lon - 1) / (int) (Integer.parseInt(conf.get("lat-long-range")) / Math.sqrt(sensorAreas)))
                                    + ")";
                        } else {
                            throw new IllegalArgumentException("The square root of the number of areas must not have a remainder!");
                        }

                        Septet<String, Double, String, String, String, Double, Double> sensorData = Septet.with(id, pm2, area, latString, longString, tempC, humid);
                        return sensorData;
                    }

                }
        );

        // Converting the input datastream to InfluxDBPoint-datastream
        final DataStream<InfluxDBPoint> influxDBRawValueSinkingDS = extractedDS.map(
                extractedQT -> {

                    // Create the timestamp
                    long timestamp = System.currentTimeMillis();

                    //Set the tags
                    HashMap<String, String> tags = new HashMap<>();
                    tags.put("id", extractedQT.getValue0());
                    tags.put("area", extractedQT.getValue2());

                    //Set the fields
                    HashMap<String, Object> fields = new HashMap<>();
                    fields.put("pm2", extractedQT.getValue1());
                    fields.put("humidity", extractedQT.getValue4());
                    fields.put("tempC", extractedQT.getValue3());

                    return new InfluxDBPoint("Pollution", timestamp, tags, fields);
                });

        influxDBRawValueSinkingDS.addSink(new InfluxDBSink(influxDBConfig));

        final DataStream<Pair<Integer, String>> processedDS = extractedDS
                .keyBy(qt2 -> qt2.getValue0())
                .window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(30)))
                .process(new DetectPM2RisePerSensor());

        final DataStream<InfluxDBPoint> influxDBSensorSinkingDS =
                ((SingleOutputStreamOperator<Pair<Integer, String>>) processedDS).getSideOutput(sensorOutput)
                        .map(ennead -> {

                            // Create the timestamp
                            long timestamp = System.currentTimeMillis();

                            //Set the tags
                            HashMap<String, String> tags = new HashMap<>();
                            tags.put("id", ennead.getValue0());
                            tags.put("area", ennead.getValue2());

                            //Set the fields
                            HashMap<String, Object> fields = new HashMap<>();
                            fields.put("pm2tooHigh", ennead.getValue1());
                            fields.put("avgPM2", ennead.getValue3());
                            fields.put("elementsInWindow", ennead.getValue4());
                            fields.put("sumPM2", ennead.getValue5());
                            fields.put("tempC", ennead.getValue6());
                            fields.put("tempF", ennead.getValue7());
                            fields.put("humidity", ennead.getValue8());

                            return new InfluxDBPoint("SensorData", timestamp, tags, fields);

                        });

        influxDBSensorSinkingDS.addSink(new InfluxDBSink(influxDBConfig));

        // execute program
        env.execute("MQTT Detection StreamingJob");
    }

    public static class DetectPM2RisePerSensor
            extends ProcessWindowFunction<Septet<String, Double, String, String, String, Double, Double>, Pair<Integer, String>, String, TimeWindow> {

        @Override
        public void process(String key, Context context, Iterable<Septet<String, Double, String, String, String, Double, Double>> input, Collector<Pair<Integer, String>> out) throws IOException {
            List<Double> pm2Values = new ArrayList<Double>();
            List<Double> tempValues = new ArrayList<Double>();
            List<Double> humidValues = new ArrayList<Double>();

            input.iterator().forEachRemaining(q -> pm2Values.add(q.getValue1()));
            input.iterator().forEachRemaining(q -> tempValues.add(q.getValue5()));
            input.iterator().forEachRemaining(q -> humidValues.add(q.getValue6()));

            int countWindowElems = pm2Values.size();

            double sumPM2 = 0d;
            for (Double pm2 : pm2Values) {
                sumPM2 = sumPM2 + pm2;
            }

            double avgPM2 = sumPM2 / countWindowElems;

            String sensorID = input.iterator().next().getValue0();
            String area = input.iterator().next().getValue2();
            double tempC = Collections.max(tempValues);
            double humid = Collections.max(humidValues);
            double tempF = tempC * 1.8 + 32;


            if (countWindowElems > 1) {

                Double boundVal = new Double(countWindowElems / 10);
                int boundaryValue = boundVal.intValue();

                List<Double> firstTenPercent = pm2Values.subList(0, 1 + boundaryValue);
                List<Double> lastTenPercent = pm2Values.subList(((countWindowElems - boundaryValue) - 1), countWindowElems);

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
                    context.output(sensorOutput, Ennead.with(sensorID, true, area, avgPM2, countWindowElems, sumPM2, tempC, tempF, humid));
                    out.collect(new Pair(1, area));
                } else {
                    context.output(sensorOutput, Ennead.with(sensorID, false, area, avgPM2, countWindowElems, sumPM2, tempC, tempF, humid));
                    out.collect(new Pair(0, area));
                }
            } else {
                context.output(sensorOutput, Ennead.with(sensorID, false, area, avgPM2, countWindowElems, sumPM2, tempC, tempF, humid));
                out.collect(new Pair(0, area));
            }

        }
    }
}