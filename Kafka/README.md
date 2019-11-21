# First steps of Kafkastreams

### Create the Topics
Kafka Topics have to be created first as following.

`bin/kafka-topics –-bootstrap-server <IP-address of Kafka Nodeport Service>:19092 --create --topic usecase-input --partitions 1 --replication-factor 1`

`bin/kafka-topics –-bootstrap-server <IP-address of Kafka Nodeport Service>:19092 --create --topic usecase-output --partitions 1 --replication-factor 1`

`bin/kafka-topics –-bootstrap-server <IP-address of Kafka NodePort Service>:19092 --create --topic usecase-outputMax --partitions 1 --replication-factor 1`

### Build .jar File
Direct in to the directory.

`mvn package`

### Start UsecaseExample(Consumer)

`java -cp target/uber-kafka-streams-usecase-1.0-SNAPSHOT.jar -DLOGLEVEL=INFO com.example.streams.usecase.UseCaseExample`

### Start Producer
Open a new terminal. Go in to the directory.

 `java -cp target/uber-kafka-streams-usecase-1.0-SNAPSHOT.jar -DLOGLEVEL=INFO com.example.streams.usecase.UseCaseGenProducer`

### Look at the topics

`bin/kafka-console-consumer --topic usecase-input --from-beginning --bootstrap-server <IP-address of Kafka NodePort Service>:19092 --property print.key=true`

`bin/kafka-console-consumer --topic usecase-output --from-beginning --bootstrap-server <IP-address of Kafka NodePort Service>:19092 --property print.key=true`

`bin/kafka-console-consumer --topic usecase-outputMax --from-beginning --bootstrap-server <IP-address of Kafka NodePort Service>:19092 --property print.key=true`

(the property part is not nessesary)
