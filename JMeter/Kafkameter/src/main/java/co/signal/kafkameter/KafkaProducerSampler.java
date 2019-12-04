/*
 * Copyright 2014 Signal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.signal.kafkameter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import com.google.common.base.Strings;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log.Logger;

/**
 * A {@link org.apache.jmeter.samplers.Sampler Sampler} which produces Kafka messages.
 *
 * @author codyaray
 * @since 6/27/14
 *
 * @see "http://ilkinbalkanay.blogspot.com/2010/03/load-test-whatever-you-want-with-apache.html"
 * @see "http://newspaint.wordpress.com/2012/11/28/creating-a-java-sampler-for-jmeter/"
 * @see "http://jmeter.512774.n5.nabble.com/Custom-Sampler-Tutorial-td4490189.html"
 */
public class KafkaProducerSampler extends AbstractJavaSamplerClient {

  private static final Logger log = LoggingManager.getLoggerForClass();

  /**
   * Parameter for setting the Kafka brokers; for example, "kafka01:9092,kafka02:9092".
   */
  private static final String PARAMETER_KAFKA_BROKERS = "kafka_brokers";

  /**
   * Parameter for setting the Kafka topic name.
   */
  private static final String PARAMETER_KAFKA_TOPIC = "kafka_topic";

  /**
   * Parameter for setting the Kafka key.
   */
  private static final String PARAMETER_KAFKA_KEY = "kafka_key";

  /**
   * Parameter for setting the Kafka message.
   */
  private static final String PARAMETER_KAFKA_MESSAGE = "kafka_message";

  /**
   * Parameter for setting Kafka's {@code serializer.class} property.
   */
  private static final String PARAMETER_KAFKA_MESSAGE_SERIALIZER = "kafka_message_serializer";

  /**
   * Parameter for setting Kafka's {@code key.serializer.class} property.
   */
  private static final String PARAMETER_KAFKA_KEY_SERIALIZER = "kafka_key_serializer";

  /**
   * Parameter for setting the Kafka ssl keystore (include path information); for example, "server.keystore.jks".
   */
  private static final String PARAMETER_KAFKA_SSL_KEYSTORE = "kafka_ssl_keystore";

  /**
   * Parameter for setting the Kafka ssl keystore password.
   */
  private static final String PARAMETER_KAFKA_SSL_KEYSTORE_PASSWORD = "kafka_ssl_keystore_password";

  /**
   * Parameter for setting the Kafka ssl truststore (include path information); for example, "client.truststore.jks".
   */
  private static final String PARAMETER_KAFKA_SSL_TRUSTSTORE = "kafka_ssl_truststore";

  /**
   * Parameter for setting the Kafka ssl truststore password.
   */
  private static final String PARAMETER_KAFKA_SSL_TRUSTSTORE_PASSWORD = "kafka_ssl_truststore_password";

  /**
   * Parameter for setting the Kafka security protocol; "true" or "false".
   */
  private static final String PARAMETER_KAFKA_USE_SSL = "kafka_use_ssl";

  /**
   * Parameter for setting encryption. It is optional.
   */
  private static final String PARAMETER_KAFKA_COMPRESSION_TYPE = "kafka_compression_type";

  /**
   * Parameter for setting the partition. It is optional.
   */
  private static final String PARAMETER_KAFKA_PARTITION = "kafka_partition";

  //private Producer<Long, byte[]> producer;

  private KafkaProducer<String, String> producer;


  @Override
  public void setupTest(JavaSamplerContext context) {
    Properties props = new Properties();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, context.getParameter(PARAMETER_KAFKA_BROKERS));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, context.getParameter(PARAMETER_KAFKA_KEY_SERIALIZER));
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, context.getParameter(PARAMETER_KAFKA_MESSAGE_SERIALIZER));
    props.put(ProducerConfig.ACKS_CONFIG, "1");

    // check if kafka security protocol is SSL or PLAINTEXT (default)
    if(context.getParameter(PARAMETER_KAFKA_USE_SSL).equals("true")){
      log.info("Setting up SSL properties...");
      props.put("security.protocol", "SSL");
      props.put("ssl.keystore.location", context.getParameter(PARAMETER_KAFKA_SSL_KEYSTORE));
      props.put("ssl.keystore.password", context.getParameter(PARAMETER_KAFKA_SSL_KEYSTORE_PASSWORD));
      props.put("ssl.truststore.location", context.getParameter(PARAMETER_KAFKA_SSL_TRUSTSTORE));
      props.put("ssl.truststore.password", context.getParameter(PARAMETER_KAFKA_SSL_TRUSTSTORE_PASSWORD));
    }

    String compressionType = context.getParameter(PARAMETER_KAFKA_COMPRESSION_TYPE);
    if (!Strings.isNullOrEmpty(compressionType)) {
      props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
    }

    producer = new KafkaProducer<String, String>(props);
  }

  @Override
  public void teardownTest(JavaSamplerContext context) {
    producer.close();
  }

  @Override
  public Arguments getDefaultParameters() {
    Arguments defaultParameters = new Arguments();
    defaultParameters.addArgument(PARAMETER_KAFKA_BROKERS, "${PARAMETER_KAFKA_BROKERS}");
    defaultParameters.addArgument(PARAMETER_KAFKA_TOPIC, "${PARAMETER_KAFKA_TOPIC}");
    defaultParameters.addArgument(PARAMETER_KAFKA_KEY, "${PARAMETER_KAFKA_KEY}");
    defaultParameters.addArgument(PARAMETER_KAFKA_MESSAGE, "${PARAMETER_KAFKA_MESSAGE}");
    defaultParameters.addArgument(PARAMETER_KAFKA_MESSAGE_SERIALIZER, "org.apache.kafka.common.serialization.StringSerializer");
    defaultParameters.addArgument(PARAMETER_KAFKA_KEY_SERIALIZER, "org.apache.kafka.common.serialization.StringSerializer");
    defaultParameters.addArgument(PARAMETER_KAFKA_SSL_KEYSTORE, "${PARAMETER_KAFKA_SSL_KEYSTORE}");
    defaultParameters.addArgument(PARAMETER_KAFKA_SSL_KEYSTORE_PASSWORD, "${PARAMETER_KAFKA_SSL_KEYSTORE_PASSWORD}");
    defaultParameters.addArgument(PARAMETER_KAFKA_SSL_TRUSTSTORE, "${PARAMETER_KAFKA_SSL_TRUSTSTORE}");
    defaultParameters.addArgument(PARAMETER_KAFKA_SSL_TRUSTSTORE_PASSWORD, "${PARAMETER_KAFKA_SSL_TRUSTSTORE_PASSWORD}");
    defaultParameters.addArgument(PARAMETER_KAFKA_USE_SSL, "${PARAMETER_KAFKA_USE_SSL}");
    defaultParameters.addArgument(PARAMETER_KAFKA_COMPRESSION_TYPE, null);
    defaultParameters.addArgument(PARAMETER_KAFKA_PARTITION, null);
    return defaultParameters;
  }

  @Override
  public SampleResult runTest(JavaSamplerContext context) {
    SampleResult result = newSampleResult();
    String topic = context.getParameter(PARAMETER_KAFKA_TOPIC);
    String key = context.getParameter(PARAMETER_KAFKA_KEY);
    String message = context.getParameter(PARAMETER_KAFKA_MESSAGE);
    sampleResultStart(result, message);

    final ProducerRecord<String, String> producerRecord;
    String partitionString = context.getParameter(PARAMETER_KAFKA_PARTITION);
    if (Strings.isNullOrEmpty(partitionString)) {
      producerRecord = new ProducerRecord<String, String>(topic, key, message);
    } else {
      final int partitionNumber = Integer.parseInt(partitionString);
      producerRecord = new ProducerRecord<String, String>(topic, partitionNumber, key, message);
    }

    try {
      producer.send(producerRecord);
      sampleResultSuccess(result, null);
    } catch (Exception e) {
      sampleResultFailed(result, "500", e);
    }
    return result;
  }

  /**
   * Use UTF-8 for encoding of strings
   */
  private static final String ENCODING = "UTF-8";

  /**
   * Factory for creating new {@link SampleResult}s.
   */
  private SampleResult newSampleResult() {
    SampleResult result = new SampleResult();
    result.setDataEncoding(ENCODING);
    result.setDataType(SampleResult.TEXT);
    return result;
  }

  /**
   * Start the sample request and set the {@code samplerData} to {@code data}.
   *
   * @param result
   *          the sample result to update
   * @param data
   *          the request to set as {@code samplerData}
   */
  private void sampleResultStart(SampleResult result, String data) {
    result.setSamplerData(data);
    result.sampleStart();
  }

  /**
   * Mark the sample result as {@code end}ed and {@code successful} with an "OK" {@code responseCode},
   * and if the response is not {@code null} then set the {@code responseData} to {@code response},
   * otherwise it is marked as not requiring a response.
   *
   * @param result sample result to change
   * @param response the successful result message, may be null.
   */
  private void sampleResultSuccess(SampleResult result, /* @Nullable */ String response) {
    result.sampleEnd();
    result.setSuccessful(true);
    result.setResponseCodeOK();
    if (response != null) {
      result.setResponseData(response, ENCODING);
    }
    else {
      result.setResponseData("No response required", ENCODING);
    }
  }

  /**
   * Mark the sample result as @{code end}ed and not {@code successful}, and set the
   * {@code responseCode} to {@code reason}.
   *
   * @param result the sample result to change
   * @param reason the failure reason
   */
  private void sampleResultFailed(SampleResult result, String reason) {
    result.sampleEnd();
    result.setSuccessful(false);
    result.setResponseCode(reason);
  }

  /**
   * Mark the sample result as @{code end}ed and not {@code successful}, set the
   * {@code responseCode} to {@code reason}, and set {@code responseData} to the stack trace.
   *
   * @param result the sample result to change
   * @param exception the failure exception
   */
  private void sampleResultFailed(SampleResult result, String reason, Exception exception) {
    sampleResultFailed(result, reason);
    result.setResponseMessage("Exception: " + exception);
    result.setResponseData(getStackTrace(exception), ENCODING);
  }

  /**
   * Return the stack trace as a string.
   *
   * @param exception the exception containing the stack trace
   * @return the stack trace
   */
  private String getStackTrace(Exception exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
