This is an IoT project conducted by students of the UAS JOANNEUM as part of the bachelor's programme of [Information Management](https://www.fh-joanneum.at/informationsmanagement/bachelor/).

# Responsible Persons

* @AnotherCodeArtist is the Mentor and guiding person of this project.

* @GregorFernbach is responsible for the ingestion and stream processing of the MQTT IoT data. His tasks are to find a MQTT broker product, deploy it and configure it to have a connection to the JMeter script which will provice the data.
Moreover the broker should have a connection to the Stream processing engine (Apache Flink). When the data enters the stream processing engine it should also be sinked in a Time-Serie Database (InfluxDB). Thus a connector in Apache Flink to the InfluxDB is needed.

* @gzei is responsible for the infrastructure (Kubernetes, NFS, Hardware and VMs).

* @vollmerm17 is responsible for Kafka and Streamprocessing with Kafka Streams.

* @lachchri16 is responsible for visualisation.

* @cynze is responsible for the databases and the connections between Kafka and those databases.

All members are responsible for the loadtesting.

# Architecture

# Requirements

* Debian 10.1 servers
* external NFS server for centralized storage
* SSDs are highly recommended

# Structure

## K8s

In this folder all necessary files for recreating the Kubernetes cluster are stored. This includes:

* The Ansible Playbook for installing the prerequisites
* An example cluster.yml used for our implementation. This cluster utilizes three nodes, where the first one is a controlplane and etcd node.
the two remaining nodes are etcd nodes as well as worker nodes.

## Kafka

## Databases

## ApacheFlink

## JMeter

In this folder a fork of the https://github.com/BrightTag/kafkameter is stored. The changes include:

* Support for generating message Keys.
* Support for setting Sensor IDs for the message.
* Inside the Generators folder, the loadgenerator implementation can be found. For build instructions please consult the corresponding .bat file.

This folder also includes the testplans (JMeter version 5.1.1) used for testing Kafka and MQTT.

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

## MQTT

## Kafka