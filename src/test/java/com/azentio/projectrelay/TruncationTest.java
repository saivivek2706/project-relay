package com.azentio.projectrelay;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import com.azentio.projectrelay.service.*;import io.micrometer.core.instrument.simple.SimpleMeterRegistry;import org.junit.jupiter.api.Test;
import java.math.BigDecimal;import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
public class TruncationTest{
 @Test void capStopsEvaluationAndStillDetectsElsewhere(){
   // low max-expansions via custom properties record (minHops etc same as defaults, maxExpansions=50)
   var cfg=new LayeringProperties(3,6,Duration.ofMinutes(30),Duration.ofHours(2),new BigDecimal("100000.00"),new BigDecimal("0.90"),new BigDecimal("0.75"),"memory",50);
   var reg=new SimpleMeterRegistry(); var det=new ChainDetector(cfg,reg); var store=new InMemoryInboundEdgeStore(cfg);
   OffsetDateTime base=OffsetDateTime.parse("2026-01-01T09:00:00Z");
   // hub: 300 inbound into HUB within window, each from distinct account, amounts >= min so they are examined
   for(int i=0;i<300;i++){ var tr=new Transfer("H"+i,String.format("%010d",4000000000L+i),"4999999999",new BigDecimal("200000.00"),base.plusMinutes(i%100)); store.add(tr); }
   var hubOut=new Transfer("HUB-OUT","4999999999","4999999998",new BigDecimal("190000.00"),base.plusMinutes(101));
   assertDoesNotThrow(()->det.findBest(hubOut, a->store.inbound(a,hubOut.occurredAt,hubOut.occurredAt().minus(cfg.window()))));
   assertTrue(reg.counter("layering.search.truncated").count()>=1, "truncation counter should increase");
   // planted chain elsewhere still detected
   store.add(new Transfer("P1","1000000001","1000000002",new BigDecimal("200000.00"),base));
   store.add(new Transfer("P2","1000000002","1000000003",new BigDecimal("194000.00"),base.plusMinutes(5)));
   var p3=new Transfer("P3","1000000003","1000000004",new BigDecimal("188000.00"),base.plusMinutes(10));
   var found=det.findBest(p3, a->store.inbound(a,p3.occurredAt,p3.occurredAt().minus(cfg.window())));
   assertTrue(found.isPresent()); assertEquals(3,found.get().transfers().size());
 }
}