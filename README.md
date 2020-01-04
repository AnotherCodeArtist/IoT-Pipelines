This is an IoT project conducted by students of the UAS JOANNEUM as part of the bachelor's programme of [Information Management](https://www.fh-joanneum.at/informationsmanagement/bachelor/). The goal was to build two IoT pipelines. One using Kafka, Kafka Connect and Kafka Streams for ingestion and stream processing. The other uses MQTT, RabbitMQ as broker and Apache Flink as stream processing engine. Both pipelines persist data in an InfluxDB (Time-Series DB) and visualize data with Grafana.

After both pipelines have been deployed, they are load tested with Jmeter.

# Responsible Persons

* @AnotherCodeArtist is the Mentor and guiding person of this project.

* @GregorFernbach is responsible for MQTT Data ingestion and Streamprocessing with Apache Flink.

* @gzei is responsible for the infrastructure (Kubernetes, NFS, Hardware and VMs).

* @vollmerm17 is responsible for Kafka and Streamprocessing with Kafka Streams.

* @lachchri16 is responsible for visualisation.

* @cynze is responsible for the databases and the connections between Kafka and those databases.

All members are responsible for the loadtesting.

# Architecture

This system uses two different sets of components for each pipeline.
A common base is the Kubernetes infrastructure.

For the first pipeline, apache kafka is used in combination with kafka streams in the following structure:

* Apache kafka: Serves as Broker for incoming requests from clients.
* Kafka Streams: Processes the incoming data.
* Kafka Connect: Responsible for persisting data (raw sensor values as well as processed data) in an InfluxDB.

For the second pipeline, RabbitMQ is used along with ApacheFlink in the following structure:

* RabbitMQ: Serves as Broker for incoming requests from clients.
* ApacheFlink: Processes the incoming data; also responsible for persisting data (raw sensor values as well as processed data) in an InfluxDB.

The combination of ApacheFlink with Apache kafka is also implemented.

The data stored inside the InfluxDB is visualized with Grafana.
The overall goal is to compare the performance of these two pipelines.

For testing, a fictional usecase was chosen. This usecase includes measurements of particulate matter within a city.  

# Requirements

* Debian 10.1 servers
* external NFS server for centralized storage
* SSDs are highly recommended 
* Apache Flink
* Maven
* Java
* IntelliJ as IDE recommended

# Structure

## K8s

In this folder all necessary files for recreating the Kubernetes cluster are stored. This includes:

* The Ansible Playbook for installing the prerequisites
* An example cluster.yml used for our implementation. This cluster utilizes three nodes, where the first one is a controlplane and etcd node.
  the two remaining nodes are etcd nodes as well as worker nodes.

## Kafka

In this folder all files necessary for the stream processing are stored. 

- UsecaseExample is the whole Streamprocessing, its calculating diffrent things of the raw sensordata and creates a new stream in  new topic.
- UsecaseGenProducer is a Producer which can be used during the developing time (instead of JMeter)
- Serde: This is necessary for the serializing and deserializing of the stream (https://github.com/gwenshap/kafka-streams-stockstats/tree/master/src/main/java/com/shapira/examples/streams/stockstats/serde)
- Models: in this directory are the POJOs defined and a own datatype CustomPair (Because Tuples or Triplets are immutable and not useful in our algorithm)
- Namespace_kafka.json is to create a namespaces called kafka.
- Streams-Deployment.yaml is the deployment of streams in a pod.
- ConnectorInluxRaw.json is the KafkaConnect deployment useed for the raw sensordata to the InfluxDB.
- ConnectorInflux.json is the KafkaConnect deployment used for the processed sensordata to the InfluxDB. 

The Connector used is from https://docs.lenses.io/connectors/sink/influx.html.

## Databases

In **Databases** all files for redeploying the databases **CrateDB**, **InfluxDB** and **OpenTSDB** on Kubernetes are stored. For both pipelines, MQTT and Kafka, only the database InfluxDB was used. The file `values.yaml`is mainly taken from the helm chart **stable/influxdb**. 

## ApacheFlink

Is the Stream Processing Engine for the MQTT pipeline.

For the deployment of Apache Flink  the official 'yml' files from the homepage (https://ci.apache.org/projects/flink/flink-docs-stable/ops/deployment/kubernetes.html) have been used. Moreover they are accessible under ApacheFlink\Deployment.

Under ApacheFlink\StreamingJobs the source code of the Streaming Jobs can be found. There are:

- DetectionJob-rmq: Represents the whole RabbitMQ Streaming Job with Persisting raw Sensor Data, processed Sensor Data and Area Output with Telegram.
- DetectionJob-rmq-loadtest: Represents the partial RabbitMQ Streaming Job which only persists raw Sensor Data and processed Sensor Data for the load test.
- DetectionJob-kafka: Represents the whole Kafka Streaming Job with Persisting raw Sensor Data, processed Sensor Data and Area Output with Telegram.
- DetectionJob-kafka-loadtest: Represents the partial Kafka Streaming Job which only persists raw Sensor Data and processed Sensor Data for the load test.

In order to create '.jar' files out of the source code, you need maven installed and have to run 'mvn clean install'.
Then you can upload the jar to flink and submit with your parallelism properties.

For configuring the RabbitMQ connector see the [Official Docs]: https://ci.apache.org/projects/flink/flink-docs-stable/dev/connectors/rabbitmq.html and the [Source Code]: https://github.com/apache/flink/tree/master/flink-connectors/flink-connector-rabbitmq/src/main/java/org/apache/flink/streaming/connectors/rabbitmq.

For configuring the Apache Kafka connector see the [Official Docs]: https://ci.apache.org/projects/flink/flink-docs-stable/dev/connectors/kafka.html, and also the [Training from Veverica]: https://training.ververica.com/exercises/toFromKafka.html.

For configuring the InfluxDB connector see the [Apache Bahir Docs]: https://bahir.apache.org/docs/flink/current/flink-streaming-influxdb/ from which this connector comes from.

For configuring the telegram API (using Botfather) see the [Official Docs]: https://core.telegram.org/

## JMeter

In this folder a fork of the https://github.com/BrightTag/kafkameter is stored. The changes include:

* Support for generating message Keys.
* Support for setting Sensor IDs for the message.
* Inside the Generators folder, the loadgenerator implementation can be found. For build instructions please consult the corresponding .bat file.

This folder also includes the testplans (JMeter version 5.1.1) used for testing Kafka and MQTT.

For the MQTT tests the [Jmeter MQTT plugin]: https://github.com/emqx/mqtt-jmeter has been used. Also the MQTT tests use kafkameter and the Java loadgenerating classes in the background. Therefor you also need these.

# Setup

## Kubernetes

1. Set up one or more VMs with Debian 10.1
2. Set up Ansible for those VMs
3. Apply Ansible Playbook provided in the k8s folder of this repository
4. Install rke 0.3.2 or newer on any node
5. Set up ssh key authentication from this node to all nodes in the cluster
6. Modify cluster.yml or generate new one (rke config)
7. Apply this config with rke up
8. Connect to the cluster with the generated kubectl config
9. Install Helm and Tiller
10. Use Helm to install nfs-client storage provider (https://github.com/kubernetes-incubator/external-storage/tree/master/nfs-client) and set as default storage class
11. Install MetalLB LoadBalancer (https://metallb.universe.tf/installation/)
12. Optional: Install Kubernetes Dashboard

## Databases

### InfluxDB

1. Create Namespace influxdb
2. Install the helm chart (https://github.com/helm/charts/tree/master/stable/influxdb) and use the namespace
3. Open InfluxDB Shell
4. Create Databases

### CrateDB

(1-7 like described in https://crate.io/a/run-your-first-cratedb-cluster-on-kubernetes-part-one/)

1. Create Namespace crate
2. Create `crate-internal-service.yaml`
3. Apply it and create a service
4. Create `crate-external-service.yaml`
5. Apply it and create a service
6. Create `crate-controller.yaml`
7. Apply it and create the stateful set
8. Install Crash (Shell for CrateDB)
9. Create Databases

### OpenTSDB

1. Create Namespace opentsdb
2. Create Secret
3. Create Persistent Volume Claim `opentsdb-pc-claim.yaml`
4. Apply PVC
5. Create `opentsdb-deployment.yaml`
6. Apply it in order to deploy OpenTSDB
7. Create `opentsdb-service.yaml`
8. Apply and create the service
9. Create Databases

## MQTT

1. Create namespace by using the provided '.yml' file
2. Configure 'values.yml' file as you need
3. Install the helm chart with 'helm install stable/rabbitmq-ha --name rmq-cluster -f .\rabbitmq-values_VX.yaml --namespace mqtt'

## Apache Flink

1. Use and Edit the provided '.yml' files from [here]: https://ci.apache.org/projects/flink/flink-docs-stable/ops/deployment/kubernetes.html.
2. Create namespace with provided '.yml' file
3. kubectl create -f flink-configuration-configmap.yml -n flink
4. kubectl create -f jobmanager-service.yml -n flink
5. kubectl create -f jobmanager-deployment.yml -n flink
6. kubectl create -f taskmanager-deployment.yml -n flink
7. kubectl create -f jobmanager-rest-service.yml -n flink


## Kafka

1. Download Helmcharts(https://github.com/confluentinc/cp-helm-charts)
2. Modify Helmcharts ( enable NodePorts etc.)
3. Create Namespace
4. Install Kafka with Helm 
5. Create topics 
6. Deploy connector for topics (Deployment for InfluxDB is necessary before this step)
7. Start JMeter or Producer(for small first tries)
8. Check topics

------

## Grafana

1. Download values.yaml from stable/grafana

2. Change type of the service to NodePort:

   ```yaml
   service:
     type: NodePort
     port: 80
     targetPort: 3000
   ```

3. Change image section to the following:

   ```yaml
   image:
     repository: gzei/grafanatest
     tag: latest
     pullPolicy: Always
   ```

4. Change persistence to the following:

   ```yaml
   persistence:
       type: pvc
       enabled: true
       storageClassName: nfs-client
       accessModes:
         - ReadWriteOnce
       size: 8Gi
   ```

5. Set initChownData to false

   ```yaml
   initChownData:
     enabled: false
   ```

6. Set adminUser and adminPassword to default credentials

   ```yaml
   adminUser: admin
   adminPassword: [default_pw]
   ```

7. Set plugin path of grafana.ini to /var/lib/plugins

   ```yaml
   grafana.ini:
     paths:
       data: /var/lib/grafana/data
       logs: /var/log/grafana
       plugins: /var/lib/plugins
       provisioning: /etc/grafana/provisioning
   ```

8. Install Grafana with the following command:
   (assuming the customized values.yaml file exists in the current working directory)

   ```
   helm install stable/grafana -f values.yaml --name grafana --namespace grafana
   ```

9. Access Grafana GUI on 172.17.100.51:XXXXX

10. Add necessary Data Sources:

    1. InfluxDB (database: kafka)
    2. InfluxDB (database: mqtt)

11. Add new dashboard and create new panel

12. In Visualization choose "Sensor Map" 

13. Add two queries like this:

    1. ```mysql
       FROM 		default 		SensorData 	WHERE +
       SELECT		field(id) +
       		field(avgPM2) +
       		field(lat) +
       		field(long) +
       GROUP BY 	+
       FORMAT AS 	Table
       ```

    2. ```mysql
       FROM 		default 		SensorAreas 	WHERE +
       SELECT 		field(tooHigh) +
       GROUP BY 	tag(area) +
       FORMAT AS 	Table
       ```

14. While inside the dashboard, change the time range in the top right corner to the value of your choice.

    Hints:

    1. *Test data can not be read by the plugin if the timestamps of all existing points in the used database are out of range!*
    2. *The gui needs a browser refresh to read the correct data after the time range has been changed!*
