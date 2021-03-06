/*
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.examples.connectandstreams.javaproducer;

import java.util.Properties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.KeyValue;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.confluent.examples.connectandstreams.avro.Location;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;



public class Driver {

    static final String INPUT_TOPIC = "javaproducer-locations";
    static final String DEFAULT_TABLE_LOCATIONS = "/usr/local/lib/table.locations";
    static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    static final String DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:8081";

    public static void main(String[] args) throws Exception {

        if (args.length > 3) {
            throw new IllegalArgumentException("usage: ... " +
                "[<bootstrap.servers> (optional, default: " + DEFAULT_BOOTSTRAP_SERVERS + ")] " +
                "[<schema.registry.url> (optional, default: " + DEFAULT_SCHEMA_REGISTRY_URL + ")] " +
                "[<table.locations> (optional, default: " + DEFAULT_TABLE_LOCATIONS + ")] ");
            }

        final String bootstrapServers = args.length > 0 ? args[0] : DEFAULT_BOOTSTRAP_SERVERS;
        final String SCHEMA_REGISTRY_URL = args.length > 1 ? args[1] : DEFAULT_SCHEMA_REGISTRY_URL;
        final String TABLE_LOCATIONS = args.length > 2 ? args[2] : DEFAULT_TABLE_LOCATIONS;

        System.out.println("Connecting to Kafka cluster via bootstrap servers " + bootstrapServers);
        System.out.println("Connecting to Confluent schema registry at " + SCHEMA_REGISTRY_URL);
        System.out.println("Reading locations table from file " + TABLE_LOCATIONS);

        final List<Location> locationsList = new ArrayList<Location>();
        try (BufferedReader br = new BufferedReader(new FileReader(TABLE_LOCATIONS))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                locationsList.add(new Location(Long.parseLong(parts[0]), parts[1], Long.parseLong(parts[2])) );
            }
        }

        final SpecificAvroSerializer<Location> locationSerializer = new SpecificAvroSerializer<>();
        final boolean isKeySerde = false;
        locationSerializer.configure(
            Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL),
            isKeySerde);

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaProducer<Long, Location> locationProducer = new KafkaProducer<Long, Location>(props, new LongSerializer(), locationSerializer);

        locationsList.forEach(t -> {
           System.out.println("Writing location information for '" + t.getId() + "' to input topic " + INPUT_TOPIC);
           ProducerRecord<Long, Location> record = new ProducerRecord<Long, Location>(INPUT_TOPIC, t.getId(), t);
           locationProducer.send(record);
        });

        locationProducer.close();
   }

}

