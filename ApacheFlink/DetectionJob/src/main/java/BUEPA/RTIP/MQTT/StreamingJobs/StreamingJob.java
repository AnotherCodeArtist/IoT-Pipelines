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

import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.util.Collector;
import org.javatuples.Triplet;
import java.io.IOException;
import java.io.Serializable;

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
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        //env.setParallelism(1);

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


        final DataStream<Triplet<String, Double, Long>> extractedDataStream = RMQstream.map(
                new RichMapFunction<String, Triplet<String, Double, Long>>() {
                    @Override
                    public Triplet<String, Double, Long> map(String s) throws Exception {
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

                        // Extract latitude
                        String sensorLAT = input[3];
                        String unformattedLATD = sensorLAT.split(":")[1];
                        String latd = unformattedLATD.replaceAll(" ", "");

                        // Extract the particulate matter
                        String sensorPM2 = input[6];
                        String unformattedPM2 = sensorPM2.split(":")[1];
                        String pm2String = unformattedPM2.replaceAll("[ }]+", "");

                        double pm2 = Double.valueOf(pm2String).doubleValue();

                        long ts = System.currentTimeMillis();

                        /**
                         try {
                         pm2 = Double.valueOf(pm2String).doubleValue();
                         } catch (NumberFormatException e) {
                         System.out.println("Could not parse pm2, some error with the sensor reading.");
                         }
                         */

                        Triplet<String, Double, Long> sensorData = Triplet.with(id, pm2, ts);
                        return sensorData;
                    }

                }
        );

        final DataStream processedDT =
        extractedDataStream
                //.filter(t -> t.getValue1() > 30)
                .keyBy(t -> t.getValue0()) // keyed by sensor IDs
                //.keyBy(0)
                //.window(SlidingProcessingTimeWindows.of(Time.seconds(30), Time.seconds(10)))
                //.timeWindow(Time.seconds(20), Time.seconds(10))
                //.timeWindow(Time.seconds(10))
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .apply(new WindowFunction<Triplet<String, Double, Long>, String, String, TimeWindow>() {

                    @Override
                    public void apply(String key,
                                      TimeWindow window,
                                      Iterable<Triplet<String, Double, Long>> input,
                                      Collector<String> out) throws Exception {
                        long count = 0;

                        for (Triplet<String, Double, Long> i : input) {
                            count++;
                        }

                        if (count > 1) {
                            out.collect("yap :D!: " + count);
                        } else {
                            out.collect("nope :(");
                        }

                    }
                });
                //.trigger(CountTrigger.of(5))
                //.process(new DetectTooHighAirPollution());
                //.writeAsText("/tmp/alarm.txt", FileSystem.WriteMode.OVERWRITE);

        processedDT.print().setParallelism(1);

        //processedDT.writeAsText("/tmp/alarm.txt", FileSystem.WriteMode.OVERWRITE);
        //extractedDataStream.writeAsText("/tmp/alarm.txt", FileSystem.WriteMode.OVERWRITE);


        // execute program
        env.execute("MQTT Detection StreamingJob");

    }

    public static class DetectTooHighAirPollution
            extends ProcessWindowFunction<Triplet<String, Double, Long>, String, String, TimeWindow> {


        @Override
        public void process(String key, Context context, Iterable<Triplet<String, Double, Long>> input, Collector<String> out) throws IOException {

            long count = 0;

            for (Triplet<String, Double, Long> i : input) {
                count++;
            }

            if (count > 1) {
                out.collect("Window:" + context.window() + " ;yap :D!: " + count);
            } else {
                out.collect("Window:" + context.window() + " nope :(");
            }

            //String test = String.valueOf(count);


            /**
             input.iterator().forEachRemaining(trplt -> {
             if (trplt.getValue1() > 30) {
             pm2s.add(trplt.getValue1());
             //sensorPM2s.put(trplt.getValue0(), trplt.getValue1());
             }
             });


             if (pm2s.size() > 2) {
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
             //out.collect(message + " " + String.valueOf(sensorPM2s.size()));
             }
             **/
        }
    }
}

