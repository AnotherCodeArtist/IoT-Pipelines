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

		final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
				.setHost("rabbitmq-service.mqtt.svc.cluster.local")
				.setPort(5672)
				.setUserName("guest")
				.setPassword("Pa55w.rd")
				.setVirtualHost("/")
				.build();

		// Initiating a Data Stream from RabbitMQ
		final DataStream<String> stream = env
				.addSource(new RMQSource<String>(
						connectionConfig,            						// config for the RabbitMQ connection
						"graz.sensors.mqtt.pm2.influxdbsinking",        	// name of the RabbitMQ queue to consume
						false,                        		// use correlation ids; can be false if only at-least-once is required
						new SimpleStringSchema()))   						// deserialization schema to turn messages into Java objects
				.setParallelism(1);             							// non-parallel Source

		// Converting the input datastream to InfluxDBPoint-datastream
		final DataStream<InfluxDBPoint> influxStream = stream.map(
				new RichMapFunction<String, InfluxDBPoint>() {
					@Override
					public InfluxDBPoint map(String s) throws Exception {

						// Extract the payload of the message
						String[] input = s.split(",");

						// Extract the sensor ID
						String sensorID = input[0];
						String id = sensorID.split(":")[1];

						// Extract the particulate matter
						String sensorPM2 = input[5];
						String pm2 = sensorPM2.split(":")[1];

						// Extract the timestamp
						String sensorTS = input[2];
						String ts = sensorTS.split(":")[1];

						// Try to parse the timestamp to long datatype
						long timestamp = System.currentTimeMillis();
						try {
							timestamp = Long.parseLong(ts);

						} catch (NumberFormatException nfe) {
							System.out.println("NumberFormatException: " + nfe.getMessage());
						}

						//Set the tags
						HashMap<String, String> tags = new HashMap<>();
						tags.put("id", id);

						HashMap<String, Object> fields = new HashMap<>();
						fields.put("pm2", pm2);

						return new InfluxDBPoint("PM2", timestamp, tags, fields);
					}
				}
		);

		//Sinking the Data Stream to InfluxDB
		InfluxDBConfig influxDBConfig = InfluxDBConfig.builder("http://influxdb.influxdb:8086", "admin", "Pa55w.rd", "mqtt")
				.batchActions(1000)
				.flushDuration(100, TimeUnit.MILLISECONDS)
				.enableGzip(true)
				.build();

		influxStream.addSink(new InfluxDBSink(influxDBConfig));


		// execute program
		env.execute("MQTT InfluxDBSinking StreamingJob");

	}
}
