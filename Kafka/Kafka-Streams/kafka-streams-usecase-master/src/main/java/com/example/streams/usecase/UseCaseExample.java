package com.example.streams.usecase;

import com.example.streams.usecase.model.UsecaseTrend;
import com.example.streams.usecase.serde.JsonSerializer;
import com.example.streams.usecase.serde.JsonDeserializer;
import com.example.streams.usecase.model.Usecase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import com.example.streams.usecase.serde.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Properties;


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
        props.put("auto.create.topics.enable", "true");


        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, Usecase> source =builder.stream("usecase-input3");

        // Trend shows an too fast increasing trend in one time window, additinal calculations are maximum Temperature in one window
        // Average and the maximum of Pm2 and the maximum of humidity in one window per Sensor, how many streams were send in one window per Sensor
        // The max temperature per Sensor during one time window in Fahrenheit and Celsius
        KStream<Windowed<String>, UsecaseTrend> trend = source
                .groupByKey()
                .windowedBy(TimeWindows.of(60000).grace(Duration.ofMillis(timeSpan)))
                .<UsecaseTrend> aggregate(UsecaseTrend::new,(k, v, usecasetrend) -> usecasetrend.calc(v)
                        ,Materialized.<String, UsecaseTrend, WindowStore<Bytes, byte []>>as("usecase-trend")
                                .withValueSerde(new UsecaseTrendSerde())
                                .withKeySerde(Serdes.String()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .mapValues(UsecaseTrend::computeA)
                .toStream();
        trend.to("usecaseOutputTrend3", Produced.keySerde(WindowedSerdes.timeWindowedSerdeFrom(String.class)));

        //Trend2 shows how many Sensors have a fastly increasing trend in one window per Area
        KStream<String, Long> trend2 =trend
                .filter((key, value) -> value.isTooHigh())
                .groupBy((key, value) -> value.getArea())
                .count(Materialized.with(Serdes.String(),Serdes.Long()))
                .toStream();
        trend2.to("usecase-outputTrend2", Produced.with(Serdes.String(), Serdes.Long()));



        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, props);
            streams.cleanUp();
            streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
      }


   //Serde for POJO Usecase
    static public final class UsecaseSerde extends WrapperSerde<Usecase> {
        public UsecaseSerde() {
            super(new JsonSerializer<Usecase>(), new JsonDeserializer<Usecase>(Usecase.class));

        }

    }

    //Serde for POJO UsecaseTrend
    static public final class UsecaseTrendSerde extends WrapperSerde<UsecaseTrend> {
        public UsecaseTrendSerde() {
            super(new JsonSerializer<UsecaseTrend>(), new JsonDeserializer<UsecaseTrend>(UsecaseTrend.class));
        }

    }


}
