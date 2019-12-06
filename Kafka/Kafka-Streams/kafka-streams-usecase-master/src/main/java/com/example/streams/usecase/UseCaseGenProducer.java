package com.example.streams.usecase;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.ProducerConfig;
import com.example.streams.usecase.serde.JsonSerializer;
import com.example.streams.usecase.model.Usecase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;



public class UseCaseGenProducer {

    public static KafkaProducer<String, Usecase> producer = null;

    private static Gson gson = new Gson();

    public static void main(String[] args) throws InterruptedException {

       JsonSerializer<Usecase> usecaseSerializer = new JsonSerializer<>();

        //public static KafkaProducer<String, Usecase> getProducer() {

            Properties configProperties = new Properties();
            configProperties.put(ProducerConfig.CLIENT_ID_CONFIG,
                    "kafka json producer");
            configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    "172.17.100.51:31090");
            configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    "com.example.streams.usecase.serde.JsonSerializer");
            configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "com.example.streams.usecase.serde.JsonSerializer");
        configProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        configProperties.put("value.serializer", usecaseSerializer.getClass().getName());
        // Starting producer

        producer = new KafkaProducer<>(configProperties);

        // initialize
        Random random = new Random();
        long iter = 0;


        while (true) {

            iter++;

            for (int i = 0; i < 1000000000; i++) {
                String sensorId = "{\"sensor\":" + (random.nextInt(10)+1+ "}");
                int lon = random.nextInt(100);
                int lat = random.nextInt(100);
                double p = 1 + ( 50 - 1 ) *random.nextDouble() ; // random var from lognormal dist with stddev = 0.25 and mean=1
                double temp = random.nextGaussian();
                int humi = i;


                // flunctuate humi sometimes

                if (iter % 10 == 0) {
                    p = p + random.nextInt(20) - 5;
                }

                String measure = "pollution";


                Usecase usecase = new Usecase(sensorId,lon,lat,temp,humi,p,measure);
                String json = gson.toJson(usecase);
                System.out.println(json);


                // Note that we are using ticker as the key - so all asks for same stock will be in same partition

                ProducerRecord<String, Usecase> record = new ProducerRecord<>("usecase-input3",sensorId, usecase);



                producer.send(record, (RecordMetadata r, Exception e) -> {

                    if (e != null) {

                        System.out.println("Error producing events");

                        e.printStackTrace();

                    }

                });

                Thread.sleep(10);
            }

        }}

    }


