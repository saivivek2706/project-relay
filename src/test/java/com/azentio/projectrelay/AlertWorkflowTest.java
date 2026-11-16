package com.azentio.projectrelay;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;
import java.util.regex.Pattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc public class AlertWorkflowTest{
 @Autowired MockMvc mvc;
 String tx(String id,String f,String to,String amt,String at){return String.format("{\"transferId\":\"%s\",\"fromAccount\":\"%s\",\"toAccount\":\"%s\",\"amount\":%s,\"occurredAt\":\"%s\"}",id,f,to,amt,at);}
 String patchBody(String s,String note,String actor){return String.format("{\"status\":\"%s\",\"note\":\"%s\",\"actor\":\"%s\"}",s,note,actor);}
 @Test void validTransitionWritesAuditAndInvalidReturns409AndNonOpenNotExtended() throws Exception{
   // build 3-hop alert
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("W1","6000000001","6000000002","200000.00","2026-05-01T09:00:00Z"))).andExpect(status().isCreated());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("W2","6000000002","6000000003","194000.00","2026-05-01T09:05:00Z"))).andExpect(status().isCreated());
   var r=mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("W3","6000000003","6000000004","188000.00","2026-05-01T09:10:00Z"))).andExpect(status().isCreated()).andReturn();
   String body=r.getResponse().getContentAsString(); long alertId=Long.parseLong(body.replaceAll(".*\"alertId\":(\\d+).*","$1"));
   // invalid: OPEN -> ESCALATED
   mvc.perform(patch("/api/alerts/"+alertId+"/status").contentType(MediaType.APPLICATION_JSON).content(patchBody("ESCALATED","skip","analyst1"))).andExpect(status().isConflict());
   // valid: OPEN -> INVESTIGATING
   mvc.perform(patch("/api/alerts/"+alertId+"/status").contentType(MediaType.APPLICATION_JSON).content(patchBody("INVESTIGATING","looking into account 6100000001","analyst1"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INVESTIGATING"));
   // audit row
   var audit=mvc.perform(get("/api/alerts/"+alertId+"/audit")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertTrue(audit.contains("INVESTIGATING")); assertTrue(audit.contains("analyst1")); assertTrue(audit.contains("looking into account XXXXXX0001"));
   // valid: INVESTIGATING -> FALSE_POSITIVE
   mvc.perform(patch("/api/alerts/"+alertId+"/status").contentType(MediaType.APPLICATION_JSON).content(patchBody("FALSE_POSITIVE","benign","analyst1"))).andExpect(status().isOk());
   // invalid: FALSE_POSITIVE -> INVESTIGATING
   mvc.perform(patch("/api/alerts/"+alertId+"/status").contentType(MediaType.APPLICATION_JSON).content(patchBody("INVESTIGATING","reopen","analyst1"))).andExpect(status().isConflict());
   // non-OPEN not extended: 4th hop should create a NEW alert, not extend this one
   var r2=mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("W4","6000000004","6000000005","182000.00","2026-05-01T09:15:00Z"))).andExpect(status().isCreated()).andReturn();
   long newId=Long.parseLong(r2.getResponse().getContentAsString().replaceAll(".*\"alertId\":(\\d+).*","$1"));
   assertNotEquals(alertId,newId);
   // audit endpoint still only has 2 rows for old alert
   var audit2=mvc.perform(get("/api/alerts/"+alertId+"/audit")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertEquals(2, audit2.split("auditId").length-1);
 }
 @Test void alertJsonContainsNoFullAccountNumber() throws Exception{
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("M1","6100000001","6100000002","200000.00","2026-05-02T09:00:00Z"))).andExpect(status().isCreated());
   mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("M2","6100000002","6100000003","194000.00","2026-05-02T09:05:00Z"))).andExpect(status().isCreated());
   var r=mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(tx("M3","6100000003","6100000004","188000.00","2026-05-02T09:10:00Z"))).andExpect(status().isCreated()).andReturn();
   long alertId=Long.parseLong(r.getResponse().getContentAsString().replaceAll(".*\"alertId\":(\\d+).*","$1"));
   String detail=mvc.perform(get("/api/alerts/"+alertId)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   String audit=mvc.perform(get("/api/alerts/"+alertId+"/audit")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   Pattern full=Pattern.compile("(?<!\\d)\\d{10,16}(?!\\d)");
   // detail contains amounts like 200000.00 (6 digits) - fine; check full account numbers absent
   assertFalse(full.matcher(detail).find(), "alert detail must not contain any full account number");
   assertFalse(full.matcher(audit).find(), "audit JSON must not contain any full account number");
   assertTrue(detail.contains("XXXXXX0001"));
   // hops fields present
   assertTrue(detail.contains("gapMinutes")); assertTrue(detail.contains("hopRetention"));
 }
}