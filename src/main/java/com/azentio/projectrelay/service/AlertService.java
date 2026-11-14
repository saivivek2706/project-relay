package com.azentio.projectrelay.service;
import com.azentio.projectrelay.domain.*;import com.azentio.projectrelay.repo.*;import com.azentio.projectrelay.web.ConflictException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;import java.util.*;
@Service public class AlertService{
 private final AlertRepository alerts; private final AlertAuditRepository audits;
 public AlertService(AlertRepository a, AlertAuditRepository au){alerts=a;audits=au;}
 public static final Map<String,Set<String>> ALLOWED=Map.of("OPEN",Set.of("INVESTIGATING"),"INVESTIGATING",Set.of("ESCALATED","FALSE_POSITIVE"));
 @Transactional public Alert patchStatus(Long id,String toStatus,String note,String actor){
   Alert a=alerts.findById(id).orElseThrow(()->new NoSuchElementException("alert not found: "+id));
   String from=a.status;
   if(!ALLOWED.getOrDefault(from,Set.of()).contains(toStatus)) throw new ConflictException("invalid transition "+from+" -> "+toStatus);
   a.status=toStatus; a.updatedAt=OffsetDateTime.now(); alerts.save(a);
   AlertAudit au=new AlertAudit(); au.alertId=id; au.fromStatus=from; au.toStatus=toStatus; au.actor=(actor==null||actor.isBlank())?"system":actor; au.note=note; au.createdAt=OffsetDateTime.now(); audits.save(au);
   return a;
 }
 public List<AlertAudit> audit(Long id){ alerts.findById(id).orElseThrow(()->new NoSuchElementException("alert not found: "+id)); return audits.findByAlertIdOrderByCreatedAtAsc(id); }
}