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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction.Context;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.util.Collector;
import org.javatuples.Triplet;

import java.util.List;


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

        // Set up a configuration for the RabbitMQ Source
        final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
                .setHost("rabbitmq-rabbitmq-ha.mqtt.svc.cluster.local")
                .setPort(5672)
                .setUserName("guest")
                .setPassword("secret")
                .setVirtualHost("/")
                .build();
        // Initiating a Data Stream from RabbitMQ
        final DataStream<String> RMQstream = env
                .addSource(new RMQSource<String>(
                        connectionConfig,            // config for the RabbitMQ connection
                        "graz.sensoren.mqtt.wholerequest",                 // name of the RabbitMQ queue to consume
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
                        String extractId = input[0];
                        String id = extractId.split(":")[1];

                        // Extract the particulate matter
                        String extractPM2 = input[5];
                        String particulateMatter = extractPM2.split(":")[1];
                        Float pm2 = null;
                        try {
                            pm2 = Float.parseFloat(particulateMatter);
                        } catch (NumberFormatException e) {
                            System.out.println("Could not parse pm2, some error with the sensor reading.");
                        }

                        // Extract the timestamp
                        String extractTS = input[6];
                        String ts = extractTS.split(":")[1];

                        //return the sensorData as String Array;
                        //String[] sensorData = {id, pm2, ts};

                        Triplet<String, Float, String> sensorData = Triplet.with(id, pm2, ts);
                        return sensorData;
                    }

                }
        );

		extractedDataStream
				.keyBy( triplet -> triplet.getValue0()) // keyed by sensor IDs
				.timeWindow(Time.minutes(5)) // still to be decided
				.process(new DetectAirPollutionIncrease());

        // execute program
        env.execute("MQTT Transformation StreamingJob");
    }

    public class DetectAirPollutionIncrease
            extends ProcessWindowFunction<Tuple2<String, Triplet>, String, String, TimeWindow> {


        @Override
        public void process(String key, Context context, Iterable<Tuple2<String, String[]>> input, Collector<String> out) {

			String message = "";

			List<String[]> pm2s; // The sensor reading values
			input.iterator().forEachRemaining(t -> pm2s.add(t.f1));
			if (pm2s.get(0)[1] - pm2s.get(pm2s.size()-1)[1])


            out.collect(message);
        }
    }
}
