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
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBConfig;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBSink;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class StreamingJob {

    public static void main(String[] args) throws Exception {

        HashMap<String, String> conf = new HashMap<String, String>();

        conf.put("rmq-hostname", "rmq-cluster-rabbitmq-ha.mqtt.svc.cluster.local");
        conf.put("rmq-port", "5672");
        conf.put("rmq-username", "guest");
        conf.put("rmq-password", "Pa55w.rd");
        conf.put("rmq-vhost", "/");
        conf.put("rmq-queuename", "graz.sensors.mqtt.pm2.influxDBSinking");

        conf.put("influx-hostname", "http://influxdb.influxdb:8086");
        conf.put("influx-username", "admin");
        conf.put("influx-password", "Pa55w.rd");
        conf.put("influx-db", "mqtt");

        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

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
                .setParallelism(1);                         // non-parallel Source        // non-parallel Source

        // Converting the input datastream to InfluxDBPoint-datastream
        final DataStream<InfluxDBPoint> influxDBSinkingDS = RMQDS.map(
                new RichMapFunction<String, InfluxDBPoint>() {
                    @Override
                    public InfluxDBPoint map(String s) throws Exception {

                        // Extract the payload of the message
                        String[] input = s.split(",");

                        // Extract the sensor ID
                        String sensorID = input[1];
                        String unformattedID = sensorID.split(":")[1];
                        String id = unformattedID.replaceAll(" ", "");

                        // Extract the longitude
                        String sensorLONG = input[2];
                        String unformattedLONGTD = sensorLONG.split(":")[1];
                        String longtd = unformattedLONGTD.replaceAll(" ", "");

                        // Extract the latitude
                        String sensorLAT = input[3];
                        String unformattedLATD = sensorLAT.split(":")[1];
                        String latd = unformattedLATD.replaceAll(" ", "");

                        // Extract the humidity
                        String sensorHUM = input[4];
                        String unformattedHUM = sensorHUM.split(":")[1];
                        String hum = unformattedHUM.replaceAll(" ", "");

                        // Extract the temperature
                        String sensorTEMP = input[5];
                        String unformattedTEMP = sensorTEMP.split(":")[1];
                        String temp = unformattedTEMP.replaceAll(" ", "");

                        // Extract the particulate matter
                        String sensorPM2 = input[6];
                        String unformattedPM2 = sensorPM2.split(":")[1];
                        String pm2 = unformattedPM2.replaceAll("[ }]+", "");

                        // Create the timestamp
                        long timestamp = System.currentTimeMillis();

                        //Set the tags
                        HashMap<String, String> tags = new HashMap<>();
                        tags.put("id", id);
                        tags.put("longitude", longtd);
                        tags.put("latitude", latd);

                        //Set the fields
                        HashMap<String, Object> fields = new HashMap<>();
                        fields.put("pm2", pm2);
                        fields.put("humidity", hum);
                        fields.put("tempC", temp);

                        return new InfluxDBPoint("Pollution", timestamp, tags, fields);
                    }
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
        env.execute("MQTT InfluxDBSinking StreamingJob");

    }
}
