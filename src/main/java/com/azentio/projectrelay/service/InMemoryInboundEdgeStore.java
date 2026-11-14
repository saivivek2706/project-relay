package com.azentio.projectrelay.service;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.Transfer;import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;import java.util.*;import java.util.concurrent.*;
@Component public class InMemoryInboundEdgeStore implements InboundEdgeStore{
 private final ConcurrentMap<String,ConcurrentSkipListSet<Transfer>> map=new ConcurrentHashMap<>(); private final LayeringProperties cfg;
 public InMemoryInboundEdgeStore(LayeringProperties cfg){this.cfg=cfg;}
 public void add(Transfer t){ map.computeIfAbsent(t.toAccount,k->new ConcurrentSkipListSet<>(Comparator.comparing((Transfer x)->x.occurredAt).thenComparing(x->x.transferId))).add(t); trim(t.toAccount,t.occurredAt); }
 public List<Transfer> inbound(String acct, OffsetDateTime before, OffsetDateTime after){
   var set=map.getOrDefault(acct,new ConcurrentSkipListSet<>(Comparator.comparing((Transfer x)->x.occurredAt).thenComparing(x->x.transferId)));
   List<Transfer> out=new ArrayList<>(); for(Transfer t:set){ if(!t.occurredAt.isBefore(after)&&t.occurredAt.isBefore(before)) out.add(t); }
   out.sort(Comparator.comparing((Transfer t)->t.occurredAt).reversed()); return out;
 }
 private void trim(String acct, OffsetDateTime now){ var set=map.get(acct); if(set!=null) set.removeIf(t->t.occurredAt.isBefore(now.minus(cfg.window()))); }
}