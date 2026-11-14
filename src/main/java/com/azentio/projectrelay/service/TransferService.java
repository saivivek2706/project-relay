package com.azentio.projectrelay.service;
import com.azentio.projectrelay.config.LayeringProperties;import com.azentio.projectrelay.domain.*;import com.azentio.projectrelay.dto.TransferRequest;import com.azentio.projectrelay.repo.*;import com.azentio.projectrelay.util.Masking;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.math.*;import java.time.OffsetDateTime;import java.util.*;
@Service public class TransferService{
 private final TransferRepository transfers; private final AlertRepository alerts; private final ChainDetector detector; private final LayeringProperties cfg;
 public TransferService(TransferRepository t,AlertRepository a,ChainDetector d,LayeringProperties c){transfers=t;alerts=a;detector=d;cfg=c;}
 public record Decision(String transferId,String decision,Long alertId){}
 @Transactional public Decision ingest(TransferRequest req){
   Optional<Transfer> ex=transfers.findById(req.transferId());
   if(ex.isPresent()){ Optional<Alert> a=findAlertContaining(req.transferId()); return new Decision(req.transferId(), a.isPresent()?"ALERT":"CLEAR", a.map(x->x.alertId).orElse(null)); }
   Transfer t=new Transfer(req.transferId(),req.fromAccount(),req.toAccount(),req.amount().setScale(2,RoundingMode.HALF_UP),req.occurredAt());
   transfers.save(t);
   // inbound lookup via SQL bounded by window
   var chain=detector.findBest(t, acct->transfers.findInbound(acct,t.occurredAt,t.occurredAt.minus(cfg.window())));
   if(chain.isEmpty()) return new Decision(req.transferId(),"CLEAR",null);
   var c=chain.get(); List<String> ids=c.transfers().stream().map(x->x.transferId).toList();
   // extension check: existing OPEN alert whose chain is strict prefix/subsequence? spec: new chain contains every transfer of existing OPEN alert
   for(Alert old: alerts.findByStatusOrderByUpdatedAtDesc("OPEN")){
     if(new HashSet<>(ids).containsAll(old.chainIds())){
        old.hops=ids.size(); old.lastAmount=c.transfers().get(ids.size()-1).amount; old.endToEndRetention=c.e2e(); old.endAt=c.transfers().get(ids.size()-1).occurredAt; old.updatedAt=OffsetDateTime.now(); old.setChainIds(ids); alerts.save(old); return new Decision(req.transferId(),"ALERT",old.alertId);
     }
   }
   Alert a=new Alert(); a.hops=ids.size(); a.firstAmount=c.transfers().get(0).amount; a.lastAmount=c.transfers().get(ids.size()-1).amount; a.endToEndRetention=c.e2e(); a.startAt=c.transfers().get(0).occurredAt; a.endAt=c.transfers().get(ids.size()-1).occurredAt; a.status="OPEN"; a.createdAt=OffsetDateTime.now(); a.updatedAt=a.createdAt; a.setChainIds(ids); alerts.save(a);
   return new Decision(req.transferId(),"ALERT",a.alertId);
 }
 private Optional<Alert> findAlertContaining(String tid){ return alerts.findAll().stream().filter(a->a.chainIds().contains(tid)).findFirst(); }
}