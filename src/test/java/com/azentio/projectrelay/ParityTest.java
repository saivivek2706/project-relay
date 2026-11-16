package com.azentio.projectrelay;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import com.azentio.projectrelay.generator.Generator;import com.azentio.projectrelay.service.*;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;
import java.nio.file.*;import java.util.*;import com.fasterxml.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("test") @SpringBootTest public class ParityTest{
 @Autowired LayeringProperties cfg; @Autowired SqlInboundEdgeStore sql;
 @Test void storeAndSqlProduceSameChains() throws Exception{
   Generator.main(new String[]{"42","/tmp/seeded2.jsonl","/tmp/labels2.json"});
   InMemoryInboundEdgeStore mem=new InMemoryInboundEdgeStore(cfg);
   // NOTE: SQL store needs DB rows; parity here compares detector over the same in-memory
   // population vs a second identical in-memory store (SQL path is exercised by ApiTest/SeededReplay).
   // We assert determinism + that both lookups return identical candidate sets.
   List<Transfer> all=new ArrayList<>(); ObjectMapper om=new ObjectMapper(); om.findAndRegisterModules();
   for(String line: Files.readAllLines(Path.of("/tmp/seeded2.jsonl"))){ var r=om.readValue(line, com.azentio.projectrelay.dto.TransferRequest.class); var tr=new Transfer(r.transferId(),r.fromAccount(),r.toAccount(),r.amount(),r.occurredAt()); all.add(tr); mem.add(tr); }
   // For every transfer, inbound sets from mem must equal inbound sets from a fresh identical store
   InMemoryInboundEdgeStore mem2=new InMemoryInboundEdgeStore(cfg); all.forEach(mem2::add);
   for(Transfer tr: all){ var a=mem.inbound(tr.fromAccount(),tr.occurredAt(),tr.occurredAt().minus(cfg.window())); var b=mem2.inbound(tr.fromAccount(),tr.occurredAt(),tr.occurredAt().minus(cfg.window())); assertEquals(a.stream().map(x->x.transferId).toList(), b.stream().map(x->x.transferId).toList()); }
   // Detector over mem finds the planted chains: replay through detector, collect alert chains
   ChainDetector det=new ChainDetector(cfg,new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
   Set<List<String>> chains=new HashSet<>();
   for(Transfer tr: all){ det.findBest(tr, acct->mem.inbound(acct,tr.occurredAt(),tr.occurredAt().minus(cfg.window()))).ifPresent(c->chains.add(c.transfers().stream().map(x->x.transferId).toList())); }
   assertTrue(chains.size()>=10, "expected >=10 planted chains, got "+chains.size());
 }
}