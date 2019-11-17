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
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.javatuples.Triplet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class StreamingJob {

    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        OutputTag<String> outputTag = new OutputTag<String>("side-output") {
        };


        // Set up a configuration for the RabbitMQ Source
        final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
                .setHost("rmq-cluster-rabbitmq-ha.mqtt.svc.cluster.local")
                .setPort(5672)
                .setUserName("guest")
                .setPassword("Pa55w.rd")
                .setVirtualHost("/")
                .build();
        // Initiating a Data Stream from RabbitMQ
        final DataStream<String> RMQstream = env
                .addSource(new RMQSource<String>(
                        connectionConfig,            // config for the RabbitMQ connection
                        "graz.sensors.mqtt.pm2.detection",                 // name of the RabbitMQ queue to consume
                        false,                        // use correlation ids; can be false if only at-least-once is required
                        new SimpleStringSchema()))   // deserialization schema to turn messages into Java objects
                .setParallelism(1);              // parallel Source

        //Extraction of values of the Data Stream


        final DataStream<Triplet<String, Float, String>> extractedDataStream = RMQstream.map(
                new RichMapFunction<String, Triplet<String, Float, String>>() {
                    @Override
                    public Triplet<String, Float, String> map(String s) throws Exception {
                        String[] input = s.split(","); // Getting a string array of sensor data in form "id":1234

                        // Extract the ID
                        String sensorID = input[0];
                        String id = sensorID.split(":")[1];

                        // Extract the particulate matter
                        String sensorPM2 = input[1];
                        String pm2STR = sensorPM2.split(":")[1];

                        // Extract the timestamp
                        String sensorTS = input[2];
                        String ts = sensorTS.split(":")[1];

                        Float pm2 = null;
                        try {
                            pm2 = Float.parseFloat(pm2STR);
                        } catch (NumberFormatException e) {
                            System.out.println("Could not parse pm2, some error with the sensor reading.");
                        }


                        Triplet<String, Float, String> sensorData = Triplet.with(id, pm2, ts);
                        return sensorData;
                    }

                }
        );

        extractedDataStream
                .filter(t -> t.getValue1() > 30)
                .keyBy(t -> t.getValue0()) // keyed by sensor IDs
                .timeWindow(Time.seconds(15)) // still to be decided
                .process(new DetectTooHighAirPollution())
                .writeAsText("/tmp/alarm.txt", FileSystem.WriteMode.OVERWRITE);
        //.print();

        // execute program
        env.execute("MQTT Detection StreamingJob");

    }

    public static class DetectTooHighAirPollution
            extends ProcessWindowFunction<Triplet<String, Float, String>, String, String, TimeWindow> {


        @Override
        public void process(String key, Context context, Iterable<Triplet<String, Float, String>> input, Collector<String> out) throws IOException {

            Map<String, Float> sensorPM2s = new HashMap<String, Float>();
            ArrayList<Float> pm2s = new ArrayList<Float>();
            //String allSensorIDs = "";
            String message;

            input.forEach(t -> pm2s.add(t.getValue1()));

            /**
             input.iterator().forEachRemaining(trplt -> {
             if (trplt.getValue1() > 30) {
             pm2s.add(trplt.getValue1());
             //sensorPM2s.put(trplt.getValue0(), trplt.getValue1());
             }
             });
             **/

            if (sensorPM2s.size() > 2) {
                    message = "ALARM!";
                //ArrayList<String> keyList = new ArrayList<String>(sensorPM2s.keySet());
                //keyList.forEach( str -> allSensorIDs = allSensorIDs + allSensorIDs.concat(", " + str));
                //String message = "ALARM !! Sensors with ID ; recognize a too high PM2,5 concentration";
                //String urlString =  + message;
                //URL url = new URL(urlString);
                //URLConnection con = url.openConnection();
                //HttpURLConnection http = (HttpURLConnection)con;
                //http.setRequestMethod("POST");
                //http.setDoOutput(true);
                //HttpClient httpclient = HttpClients.createDefault();
                // HttpPost httppost = new HttpPost(urlString);
                //HttpResponse response = httpclient.execute(httppost);
                //HttpEntity entity = response.getEntity();
            }
            out.collect(message + sensorPM2s.size());
        }
    }
}

