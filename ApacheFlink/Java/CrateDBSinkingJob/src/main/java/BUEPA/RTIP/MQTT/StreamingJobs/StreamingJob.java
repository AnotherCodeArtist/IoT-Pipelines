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
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.io.jdbc.JDBCOutputFormat;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.apache.flink.types.Row;
import org.javatuples.Triplet;
import scala.Tuple3;

import java.util.HashMap;
import java.util.stream.Stream;

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
		ParameterTool parameters = ParameterTool.fromArgs(args);

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
						"graz.sensors.mqtt.pm2.cratedbsinking",        	// name of the RabbitMQ queue to consume
						false,                        		// use correlation ids; can be false if only at-least-once is required
						new SimpleStringSchema()))   						// deserialization schema to turn messages into Java objects
				.setParallelism(1);

		// non-parallel Source
		final DataStream<Triplet<String, String, Long>> RMQStream = stream.map(
				new RichMapFunction<String, Triplet<String, String, Long>>() {
					@Override
					public Triplet<String, String, Long> map(String s) throws Exception {

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

						new Row (3,[id,pm2,timestamp]);
						return new Triplet(id, pm2, timestamp);
					}
				}
		);

		RMQStream.writeUsingOutputFormat(createJDBCOutputFormat(parameters));

		// execute program
		env.execute("Flink CrateDB Sinking Job");
	}

	private static OutputFormat<Row> createJDBCOutputFormat(ParameterTool parameters) {
		return JDBCOutputFormat.buildJDBCOutputFormat()
				.setDrivername( "io.crate.client.jdbc.CrateDriver" )
				.setDBUrl(
						String.format( "crate://%s/" , parameters.getRequired( "crate.hosts" ))
				)
				.setBatchInterval(parameters.getInt( "batch.interval.ms" , 5000))
				.setUsername(parameters.get( "crate.user" , "crate" ))
				.setPassword(parameters.get( "crate.password" , "" ))
				.setQuery(
						String.format(
								"INSERT INTO %s (payload) VALUES (?)" ,
								parameters.getRequired( "crate.table" ))
				)
				.finish();
	}

	private static Row convertToRow(Stream<Triplet<String, String, Long>> rmqstream){
		rmqstream.map( tplt ->
		);

	}

}
