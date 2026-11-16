package com.azentio.projectrelay;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import com.azentio.projectrelay.service.ChainDetector;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.OffsetDateTime;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
public class ChainDetectorTest{
 ChainDetector d=new ChainDetector(new LayeringProperties(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry()); OffsetDateTime t0=OffsetDateTime.parse("2026-01-01T09:00:00Z");
 Transfer t(String id,String f,String to,String amt,int mins){return new Transfer(id,f,to,new BigDecimal(amt),t0.plusMinutes(mins));}
 @Test void gapExactly30Qualifies(){ var last=t("3","C","D","180000",60); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","190000",30)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isPresent()); }
 @Test void gap30m1sFails(){ var last=t("3","C","D","180000",61); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","190000",30)),"B",List.of(t("1","A","B","200000",0))); // gap 31 min >30 so no chain ; also need 3 hops: use adjusted
   assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
 @Test void hopRetentionBoundary(){ var last=t("3","C","D","180000",10); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","200000",5)),"B",List.of(t("1","A","B","200000",0))); // 180/200=0.90 qualifies, 200/200=1.0
   assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isPresent()); }
 @Test void hopRetentionBelowFails(){ var last=t("3","C","D","179980",10); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","200000",5)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
 @Test void e2eBoundary(){ var last=t("3","C","D","150000",10); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","180000",5)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isPresent()); }
 @Test void e2eBelowFails(){ var last=t("3","C","D","149980",10); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","180000",5)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
 @Test void twoHopNotFlagged(){ var last=t("2","B","C","190000",5); Map<String,List<Transfer>> m=Map.of("B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
 @Test void retentionAboveOneDoesNotQualify(){ var last=t("3","C","D","210000",10); Map<String,List<Transfer>> m=Map.of("C",List.of(t("2","B","C","200000",5)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
 @Test void tieBreakEarlierT1Wins(){ var last=t("LAST","D","E","188000",10); Map<String,List<Transfer>> m=Map.of("D",List.of(t("A2","B","D","194000",5),t("B2","Y","D","194000",6)),"B",List.of(t("A1","A","B","200000",0)),"Y",List.of(t("B1","X","Y","200000",1))); var c=d.findBest(last, a->m.getOrDefault(a,List.of())); assertTrue(c.isPresent()); assertEquals(List.of("A1","A2","LAST"),c.get().transfers().stream().map(x->x.transferId).toList()); }
 @Test void revisitsAccountNotFlagged(){ var last=t("3","A","D","180000",10); Map<String,List<Transfer>> m=Map.of("A",List.of(t("2","B","A","190000",5)),"B",List.of(t("1","A","B","200000",0))); assertTrue(d.findBest(last, a->m.getOrDefault(a,List.of())).isEmpty()); }
}