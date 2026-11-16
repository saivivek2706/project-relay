package com.azentio.projectrelay.kafka;
import com.azentio.projectrelay.dto.TransferRequest;import com.azentio.projectrelay.service.TransferService;import com.fasterxml.jackson.databind.ObjectMapper;import jakarta.validation.*;import org.apache.kafka.clients.consumer.ConsumerRecord;import org.apache.kafka.clients.producer.ProducerRecord;import org.slf4j.*;import org.springframework.kafka.annotation.KafkaListener;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.kafka.support.Acknowledgment;import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;import java.util.Set;\nimport java.util.concurrent.TimeUnit;
@Component @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(org.springframework.kafka.core.KafkaTemplate.class) public class TransferConsumer{
 private static final Logger log=LoggerFactory.getLogger(TransferConsumer.class);
 private final TransferService svc; private final KafkaTemplate<String,String> kafka; private final ObjectMapper mapper; private final Validator validator;
 public TransferConsumer(TransferService s,KafkaTemplate<String,String> k,ObjectMapper m,Validator v){svc=s;kafka=k;mapper=m;validator=v;}
 @KafkaListener(topics="transfers.v1", groupId="relay")
 public void onMessage(ConsumerRecord<String,String> rec, Acknowledgment ack) throws Exception{
   try{
     TransferRequest req=mapper.readValue(rec.value(),TransferRequest.class);
     if (rec.key() == null || !rec.key().equals(req.fromAccount())) throw new IllegalArgumentException("Kafka key must equal fromAccount");
     Set<ConstraintViolation<TransferRequest>> v=validator.validate(req); if(!v.isEmpty()) throw new IllegalArgumentException("validation: "+v);
     svc.ingest(req); ack.acknowledge();
   }catch(Exception e){
     log.warn("Invalid transfer event rejected; publishing sanitized reason to DLQ");
     ProducerRecord<String,String> dlq=new ProducerRecord<>("transfers.v1.dlq",rec.key(),rec.value());
     dlq.headers().add("x-error-reason", "invalid transfer event".getBytes(StandardCharsets.UTF_8));
     dlq.headers().add("x-original-topic","transfers.v1".getBytes(StandardCharsets.UTF_8));
     kafka.send(dlq).get(10, TimeUnit.SECONDS); ack.acknowledge();
   }
 }
}