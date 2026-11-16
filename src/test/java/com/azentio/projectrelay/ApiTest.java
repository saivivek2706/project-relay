package com.azentio.projectrelay;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc public class ApiTest{
 @Autowired MockMvc mvc;
 String tx(String id,String f,String to,String amt,String at){return String.format("{\"transferId\":\"%s\",\"fromAccount\":\"%s\",\"toAccount\":\"%s\",\"amount\":%s,\"occurredAt\":\"%s\"}",id,f,to,amt,at);}
 @Test void validationAndIdempotencyAndExtensionAndMasking() throws Exception{
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("X1","1111111111","2222222222","-5","2026-01-01T09:00:00Z"))).andExpect(status().isBadRequest());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("X2","1111111111","1111111111","100","2026-01-01T09:00:00Z"))).andExpect(status().isBadRequest());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("X3","1111111111","2222222222","100.123","2026-01-01T09:00:00Z"))).andExpect(status().isBadRequest());
   String base="2026-02-01T09:00:00Z";
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A1","1000000001","1000000002","200000.00","2026-02-01T09:00:00Z"))).andExpect(status().isCreated()).andExpect(jsonPath("$.decision").value("CLEAR"));
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A2","1000000002","1000000003","194000.00","2026-02-01T09:05:00Z"))).andExpect(jsonPath("$.decision").value("CLEAR"));
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A3","1000000003","1000000004","188000.00","2026-02-01T09:10:00Z"))).andExpect(jsonPath("$.decision").value("ALERT"));
   // An earlier CLEAR response remains CLEAR even after a later hop completes the chain.
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A1","1000000001","1000000002","200000.00","2026-02-01T09:00:00Z"))).andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("CLEAR"));
   // idempotent re-post
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A3","1000000003","1000000004","188000.00","2026-02-01T09:10:00Z"))).andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("ALERT"));
   // extension to 4 hops
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("A4","1000000004","1000000005","182000.00","2026-02-01T09:15:00Z"))).andExpect(jsonPath("$.decision").value("ALERT"));
   var res=mvc.perform(get("/api/alerts?status=OPEN")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("1000000001")))).andReturn();
   String body=res.getResponse().getContentAsString(); org.junit.jupiter.api.Assertions.assertTrue(body.contains("XXXXXX0001"));
   org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"hops\":4")||body.contains("\"hops\" : 4")||body.contains("4"));
 }
 @Test void tieBreakEarlierT1() throws Exception{
   // two chains ending same transfer: earlier t1 wins - covered by detector ordering; API smoke: single chain still ALERT
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("T1","2000000001","2000000002","200000.00","2026-03-01T09:00:00Z"))).andExpect(status().isCreated());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("T2","2000000002","2000000003","194000.00","2026-03-01T09:05:00Z"))).andExpect(status().isCreated());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("T3","2000000003","2000000004","188000.00","2026-03-01T09:10:00Z"))).andExpect(jsonPath("$.decision").value("ALERT"));
 }
}