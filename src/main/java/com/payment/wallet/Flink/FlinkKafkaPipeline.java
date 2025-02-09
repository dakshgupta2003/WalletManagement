package com.payment.wallet.Flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import java.util.Properties;
import java.util.logging.Logger;

public class FlinkKafkaPipeline {
    private final static Logger logger = Logger.getLogger(FlinkKafkaPipeline.class.getName());

    public static void main(String[] args) throws Exception { // main method to run flik job

        // env for flink job
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(); 
        // initializes the flink runtime env , required for defining sources, transformations, sinks

        Properties kafkaProperties = new Properties();
        
        {
            kafkaProperties.setProperty("bootstrap.servers", "localhost:9092");
            kafkaProperties.setProperty("group.id", "flink-kafka-consumer");
        }
        // bootstrap.servers -> kafka broker address
        // group.id -> consumer group for flink

        // consume from kafka topics
        KafkaSource<String> userConsumer = KafkaSource.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setTopics("user-creation")
            .setGroupId("flink-kafka-consumer")
            .setStartingOffsets(OffsetsInitializer.earliest()) // starts reading from earliest avalable message
            .setValueOnlyDeserializer(new SimpleStringSchema()) // simpleStringSchema() -> deserializes the kafka message to string
            .build();

        // reads data from user-creation kafka topic


        KafkaSource<String> transactionConsumer = KafkaSource.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setTopics("fund-transfer")
            .setGroupId("flink-kafka-consumer")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();
        
        // create data stream from kafka topics
        DataStream<String> userStream = env.fromSource(userConsumer, 
            WatermarkStrategy.noWatermarks(), 
            "User Kafka Source"
        );
        // creates flink stream from userConsumer
        // name of stream -> "User Kafka Source"
        
        DataStream<String> transactionStream = env.fromSource(transactionConsumer,
            WatermarkStrategy.noWatermarks(),
            "Transaction Kafka Source"
        );

        // process data streams
        DataStream<String> processedUserStream = userStream.map(user -> {
            logger.info("Processing user: " + user);
            return user;
        });
        // processes each user record

        DataStream<String> processedTransactionStream = transactionStream.map(transaction -> {
            logger.info("Processing transaction: " + transaction);
            return transaction;
        });



        //kafka sink for processed data streams
        KafkaSink<String> userSink= KafkaSink.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                            .setTopic("user-creation-processed")
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build()
                    )
                    .build();

        // writes processed user data to new kafka topic user-creation-processed

        KafkaSink<String> transactionSink = KafkaSink.<String>builder()
            .setBootstrapServers("localhost:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                            .setTopic("fund-transfer-processed")
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build()
                    )
                    .build();


        // sink processed data streams to kafka
        processedUserStream.sinkTo(userSink);
        processedTransactionStream.sinkTo(transactionSink);
        // sends the processed data to new kafka topics

        // execute flink job
        env.execute("Flink Kafka Pipeline");
    }

    

}
