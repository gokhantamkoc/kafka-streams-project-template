package com.gokhantamkoc.kafka.streams.projecttemplate;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class StreamsStarterApp {
	public static void main(String[] args) {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        System.out.println("Kafka Streams Template Project Started...");
		Properties config = new Properties();
		/*
		 * application.id config is also group-id for consumer group.
		 * will be used for:
		 * 1) the default client-id prefix
		 * 2) the group-id for membership management
		 * 3) the changelog topic prefix.
		 */
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-starter-app");
        /*
         * bootstrap.servers: Streams app should connect to a kafka server. i.e. host:port,host:port
         */
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        
        /*
         *	auto.offset.reset.config: set to earliest to consume the topic from the start
         */
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        /*
         * default.[key|value].serde
         * serde stands for Serialization and Deserialization of data
         */
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        /* Topology of our Stream App with HSL
         * 1. Stream from Kafka					< null, "Kafka Kafka Streams">
         * 2. MapValues lowercase				< null, "kafka kafka streams">
         * 3. FlatMapValues split space			< null, "kafka">, < null, "kafka">, < null, "streams">
         * 4. SelectKey to apply a key			< "kafka", "kafka">, < "kafka", "kafka">, < "streams", "streams">
         * 5. GroupByKey before aggregation		(< "kafka", "kafka">, < "kafka", "kafka">), (< "streams", "streams">)
         * 6. Count occurences in each Group	<"kafka", 2>, <"streams", 1>
         */
        
        final StreamsBuilder builder = new StreamsBuilder();

        // 1 stream from kafka
        KStream<String, String> wordCountInputStream = builder.stream("word-count-input");
        
        // 2 map values
        KTable<String, Long> counter = wordCountInputStream.mapValues(value -> value.toLowerCase())
        
        // 3 flat map values split by space
        .flatMapValues(textValue -> Arrays.asList(textValue.split(" ")))
        .selectKey((key, word) -> word)
        .groupByKey()
        .count();
        counter.mapValues((k, v) -> k + " count: " + v.toString()).toStream().to("word-count-output");


        try {
            Topology topology = builder.build();
            // System.out.println("Topology:\n" + topology.describe());
            KafkaStreams streams = new KafkaStreams(topology, config);
            streams.cleanUp();
            streams.start();
            streams.localThreadsMetadata().forEach(data -> System.out.println(data));
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // print the topology

        // shutdown hook to correctly close the streams application
        // Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}
}
