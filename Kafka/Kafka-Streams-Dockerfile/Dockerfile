FROM openjdk:8
COPY uber-KafkaStreamTest-1.0-SNAPSHOT.jar /usr/bin/
WORKDIR /usr/bin/
ENV HELLO_MESSAGE=$HELLO
ENV KAFKA_BROKER_LIST=127.0.0.1
ENV INPUT_TOPIC=streams_input
ENV OUTPUT_TOPIC=streams_output

CMD ["java", "-cp", "uber-KafkaStreamTest-1.0-SNAPSHOT.jar", "com.example.kafkatest.kafkatest"]
