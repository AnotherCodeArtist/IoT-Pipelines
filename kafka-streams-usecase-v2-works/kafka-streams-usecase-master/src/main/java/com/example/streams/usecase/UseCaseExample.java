package com.example.streams.usecase;

import com.example.streams.usecase.model.UsecaseMaxAvg;
import com.example.streams.usecase.serde.JsonSerializer;
import com.example.streams.usecase.serde.JsonDeserializer;
import com.example.streams.usecase.model.UsecaseStats;
import com.example.streams.usecase.model.Usecase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import com.example.streams.usecase.serde.*;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class UseCaseExample {


    public static void main(String[] args) throws Exception{

        JsonDeserializer<Usecase> usecaseDeserializer = new JsonDeserializer<>();
        Properties props = new Properties();

        int timeSpan = 10000;


        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "usecase");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "172.17.100.51:31090");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, UsecaseSerde.class.getName());
       props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"com.example.streams.usecase.serde.JsonDeserializer");
       props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"com.example.streams.usecase.serde.JsonDeserializer");
       props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
       props.put("value.deserializer", usecaseDeserializer.getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // work-around for an issue around timing of creating internal topics
        // Fixed in Kafka 0.10.2.0
        // don't use in large production apps - this increases network load
        // props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Usecase> source =builder.stream("usecase-input");


        KStream<Windowed<String>, UsecaseStats> stats = source
                .groupByKey()
                .windowedBy(TimeWindows.of(5000).advanceBy(1000))
                .<UsecaseStats>aggregate(() -> new UsecaseStats(), (k, v, usecasestats) -> usecasestats.add(v),
                        Materialized.<String, UsecaseStats, WindowStore<Bytes, byte[]>>as("usecase-aggregates")
                                .withValueSerde(new UsecaseStatsSerde()))
                .toStream()
                .mapValues((usecase) -> usecase.huhu());

 //   System.out.println(stats.toString());
          stats.to("usecase-output", Produced.keySerde(WindowedSerdes.timeWindowedSerdeFrom(String.class)));


        KStream<Windowed<String>, UsecaseMaxAvg> max = source
                .groupByKey()
                .windowedBy(TimeWindows.of(120000).advanceBy(timeSpan))
                .<UsecaseMaxAvg>aggregate(() -> new UsecaseMaxAvg(), (k, v, usecasemaxavg) -> usecasemaxavg.max(v),
                        Materialized.<String, UsecaseMaxAvg, WindowStore<Bytes, byte[]>>as("usecase-max")
                                .withValueSerde(new UsecaseMaxAvgSerde()))
                .toStream()
                .mapValues((use) -> use.computeAvgPrice())
                .filter((key, value) -> isLatest(key, timeSpan));

        //   System.out.println(stats.toString());
        max.to("usecase-outputMax", Produced.keySerde(WindowedSerdes.timeWindowedSerdeFrom(String.class)));
/*
        //wenn der sensor innerhalb einer Minute nichts sendet
        final KStream<Windowed<Usecase>, Long> sensorsOff = source.map((key, sensorId) -> {
            System.out.println("key: " + key);
            System.out.println("sendorId: " + sensorId);
            return new KeyValue<>(sensorId, key);
        })
                .groupByKey()
                .windowedBy(TimeWindows.of(TimeUnit.MINUTES.toMillis(1)))
                .count()
                .filter((windowedId, count) -> count == 0)
                .toStream();

        sensorsOff.to("usecase-outputOff", Produced.keySerde(WindowedSerdes.timeWindowedSerdeFrom(Usecase.class)));

*/
        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, props);
            streams.cleanUp();
            streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
      }


    private static boolean isLatest(Windowed<String> key, int timeSpan) {
        long endMs = key.window().end();
        long timestamp = getTimestampOfEvent();

        return (Math.ceil((float)timestamp / timeSpan)) * timeSpan == endMs;

    }


    private static long getTimestampOfEvent() {
        Date date= new Date();
        long timestamp = date.getTime();
        return timestamp;
    }

    static public final class UsecaseSerde extends WrapperSerde<Usecase> {
        public UsecaseSerde() {
            super(new JsonSerializer<Usecase>(), new JsonDeserializer<Usecase>(Usecase.class));

        }

    }
          static public final class UsecaseStatsSerde extends WrapperSerde<UsecaseStats> {
              public UsecaseStatsSerde() {
                  super(new JsonSerializer<UsecaseStats>(), new JsonDeserializer<UsecaseStats>(UsecaseStats.class));
              }

          }

    static public final class UsecaseMaxAvgSerde extends WrapperSerde<UsecaseMaxAvg> {
        public UsecaseMaxAvgSerde() {
            super(new JsonSerializer<UsecaseMaxAvg>(), new JsonDeserializer<UsecaseMaxAvg>(UsecaseMaxAvg.class));
        }

    }
}
