package com.azentio.projectrelay;
import org.apache.kafka.clients.consumer.ConsumerConfig;import org.apache.kafka.clients.consumer.KafkaConsumer;import org.apache.kafka.clients.producer.*;import org.apache.kafka.common.serialization.StringDeserializer;import org.apache.kafka.common.serialization.StringSerializer;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;import org.springframework.kafka.test.EmbeddedKafkaBroker;import org.springframework.kafka.test.context.EmbeddedKafka;import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;import org.springframework.test.web.servlet.MockMvc;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.time.Duration;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ActiveProfiles("test") @SpringBootTest(properties={"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}","spring.autoconfigure.exclude="}) @AutoConfigureMockMvc @EmbeddedKafka(partitions=1, topics={"transfers.v1","transfers.v1.dlq"}) @DirtiesContext public class KafkaTest{
 @Autowired EmbeddedKafkaBroker broker; @Autowired MockMvc mvc;
 @Test void plantedMalformedDuplicate() throws Exception{
   var props=new Properties(); props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,broker.getBrokersAsString()); props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class); props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
   try(var prod=new KafkaProducer<String,String>(props)){
     String base="2026-04-01T09:00:00Z";
     prod.send(new ProducerRecord<>("transfers.v1","1000000001","{\"transferId\":\"K1\",\"fromAccount\":\"1000000001\",\"toAccount\":\"1000000002\",\"amount\":200000.00,\"occurredAt\":\"2026-04-01T09:00:00Z\"}")).get();
     prod.send(new ProducerRecord<>("transfers.v1","1000000002","{\"transferId\":\"K2\",\"fromAccount\":\"1000000002\",\"toAccount\":\"1000000003\",\"amount\":194000.00,\"occurredAt\":\"2026-04-01T09:05:00Z\"}")).get();
     prod.send(new ProducerRecord<>("transfers.v1","1000000003","{\"transferId\":\"K3\",\"fromAccount\":\"1000000003\",\"toAccount\":\"1000000004\",\"amount\":188000.00,\"occurredAt\":\"2026-04-01T09:10:00Z\"}")).get();
     prod.send(new ProducerRecord<>("transfers.v1","bad","not-json{")).get();
     prod.send(new ProducerRecord<>("transfers.v1","1000000003","{\"transferId\":\"K3\",\"fromAccount\":\"1000000003\",\"toAccount\":\"1000000004\",\"amount\":188000.00,\"occurredAt\":\"2026-04-01T09:10:00Z\"}")).get();
     prod.flush();
   }
   // wait for alert
   String body=null; for(int i=0;i<30;i++){ Thread.sleep(500); var r=mvc.perform(get("/api/alerts?status=OPEN")).andExpect(status().isOk()).andReturn(); body=r.getResponse().getContentAsString(); if(body.contains("K3")||body.contains("188000")) break; }
   assertNotNull(body); assertTrue(body.contains("XXXXXX"), "masked");
   // DLQ has malformed with header
   var cprops=new Properties(); cprops.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,broker.getBrokersAsString()); cprops.put(ConsumerConfig.GROUP_ID_CONFIG,"dlq-check-"+UUID.randomUUID()); cprops.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest"); cprops.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class); cprops.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
   try(var cons=new KafkaConsumer<String,String>(cprops)){ cons.subscribe(List.of("transfers.v1.dlq")); var recs=cons.poll(Duration.ofSeconds(10)); assertTrue(recs.count()>=1); assertNotNull(recs.iterator().next().headers().lastHeader("x-error-reason")); }
 }
}