# First steps

## Deployment of Kafka on K8s
Download the helm carts. https://github.com/confluentinc/cp-helm-charts

Direct to the cp-helm charts and install kafka.

`helm install ./ --namespace kafka --name kafka01 -f values.yaml`

### Create the Topics
Kafka Topics have to be created first as following.

`bin/kafka-topics –-bootstrap-server <IP-address of Kafka Nodeport Service>:19092 --create --topic usecase-input --partitions 4 --replication-factor 1`

`bin/kafka-topics –-bootstrap-server <IP-address of Kafka Nodeport Service>:19092 --create --topic usecaseOutputTrend --partitions 4 --replication-factor 1`

### Deploy Connectors
It is important that the InfluxDB is deployed and the database is created before this step.

Connector to the Influxdatabase for the raw data.

`curl -X POST -d @connectorInfluxRaw.json http://<ClusterIp of KafkaConnect SVC>:8083/connectors -H "Content-Type: application/json"`


Connector to the Influxdatabase for the processed data.

`curl -X POST -d @connectorInflux.json http://<ClusterIp of KafkaConnect SVC>:8083/connectors -H "Content-Type: application/json"`

### Start KafkaStreams
Build a Container for streams.

`kubectl apply -f deployment.yaml`


### Start Producer (For Simulation < Use JMeter )
Open a new terminal. Go in to the directory.

 `java -cp target/uber-kafka-streams-usecase-1.0-SNAPSHOT.jar -DLOGLEVEL=INFO com.example.streams.usecase.UseCaseGenProducer`

### Look at the topics

`bin/kafka-console-consumer --topic usecase-input --from-beginning --bootstrap-server <IP-address of Kafka NodePort Service>:19092 --property print.key=true`

`bin/kafka-console-consumer --topic usecaseOutputTrend --from-beginning --bootstrap-server <IP-address of Kafka NodePort Service>:19092 --property print.key=true`
