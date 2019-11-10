package com.example.kafkatest;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.concurrent.CountDownLatch;
public class kafkatest {
    public static void main(String[] args) {
        System.out.println(System.getenv("HELLO_MESSAGE"));
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams_test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BROKER_LIST"));
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, String> source = builder.stream(System.getenv("INPUT_TOPIC"));

        JSONParser parser = new JSONParser();
        source.flatMap((k,v) -> {
            List<KeyValue<String,String>> tmp = new ArrayList<KeyValue<String, String>>();
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) parser.parse(v);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //Da drinnen kann ma die sachen machen die ma will....
            System.out.println(jsonObject.get("temperature").toString());
            jsonObject.put("temperatureF",(double)jsonObject.get("temperature")*(1.8)+ 32);
            tmp.add(new KeyValue(k, jsonObject.toJSONString()));
            return tmp;
        }).to(System.getenv("OUTPUT_TOPIC"));

        // need to override value serde to Long type
        //counts.toStream().to("streams-wordcount-output");

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
