FROM openjdk:8
COPY uber-kafka-streams-usecase-1.0-SNAPSHOT.jar /usr/bin/
WORKDIR /usr/bin/
ENV KAFKA_BROKER_LIST=127.0.0.1
ENV INPUT_TOPIC=streams_input
ENV OUTPUT_TOPIC=streams_output

ENTRYPOINT ["java", "-cp", "uber-kafka-streams-usecase-1.0-SNAPSHOT.jar", "-DLOGLEVEL=WARN", "com.example.streams.usecase.UseCaseExample"]
