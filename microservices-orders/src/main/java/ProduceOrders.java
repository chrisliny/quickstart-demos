package io.confluent.examples.streams.microservices;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import java.util.Collections;

import io.confluent.examples.streams.microservices.domain.Schemas.Topics;
import static io.confluent.examples.streams.microservices.domain.beans.OrderId.id;
import static io.confluent.examples.streams.avro.microservices.OrderState.CREATED;
import static io.confluent.examples.streams.avro.microservices.Product.UNDERPANTS;
import static io.confluent.examples.streams.microservices.domain.Schemas.Topics.ORDERS;
import io.confluent.examples.streams.avro.microservices.Order;

public class ProduceOrders {

    public static void main(String[] args) throws Exception {

        final SpecificAvroSerializer<Order> mySerializer = new SpecificAvroSerializer<>();
        final boolean isKeySerde = false;
        mySerializer.configure(
            Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081"),
            isKeySerde);

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        KafkaProducer<String, Order> producer = new KafkaProducer<String, Order>(props, new StringSerializer(), mySerializer);

        int i = 1;
        while (true) {
           String orderId = id(0L);
           Order order = new Order(orderId, 15L, CREATED, UNDERPANTS, 3, 5.00d);
           ProducerRecord<String, Order> record = new ProducerRecord<String, Order>("orders", order.getId(), order);
           producer.send(record);
           Thread.sleep(1000L);
           i++;
        }
   }

}

