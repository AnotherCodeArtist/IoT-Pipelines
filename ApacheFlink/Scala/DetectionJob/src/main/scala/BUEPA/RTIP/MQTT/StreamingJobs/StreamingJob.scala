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

package BUEPA.RTIP.MQTT.StreamingJobs

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig

/**
 * Skeleton for a Flink Streaming Job.
 *
 * For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
object StreamingJob {
  def main(args: Array[String]) {
    // set up the streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val connectionConfig = new RMQConnectionConfig.Builder()
      .setHost("rabbitmq-rabbitmq-ha.mqtt.svc.cluster.local")
      .setPort(5672)
      .setUserName("guest")
      .setPassword("secret")
      .setVirtualHost("/")
      .build

    val RMQstream: DataStream[String] = env
      .addSource(new RMQSource[String](
        connectionConfig,            // config for the RabbitMQ connection
        "graz.sensoren.mqtt.wholerequest",                 // name of the RabbitMQ queue to consume
        true,                        // use correlation ids; can be false if only at-least-once is required
        new SimpleStringSchema))     // deserialization schema to turn messages into Java objects
      .setParallelism(1)               // non-parallel source is only required for exactly-once

    val processedDataStream: DataStream[Array[String]]= RMQstream.flatMap(s => s.split(","))
      .map(sarr: Array[String] => (sarr entity.attrs("temperature").value.asInstanceOf[String]))
      .keyBy(_._1)
      .window(SlidingProcessingTimeWindows.of(Time.seconds(20), Time.seconds(10)))
      .process(new MyProcessWindowFunction)



    // execute program
    env.execute("Flink Streaming Scala API Skeleton")
  }
}
