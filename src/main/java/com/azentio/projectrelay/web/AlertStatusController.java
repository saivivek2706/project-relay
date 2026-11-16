package com.azentio.projectrelay.web;
import com.azentio.projectrelay.service.AlertService;
import com.azentio.projectrelay.util.Masking;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/alerts")
public class AlertStatusController {
  private final AlertService svc;
  public AlertStatusController(AlertService s){ svc = s; }
  @PatchMapping("/{id}/status")
  public ResponseEntity<?> patch(@PathVariable Long id, @Valid @RequestBody StatusPatchRequest req){
    var a = svc.patchStatus(id, req.status(), req.note(), req.actor());
    return ResponseEntity.ok(Map.of("alertId", a.alertId, "status", a.status, "updatedAt", a.updatedAt));
  }
  @GetMapping("/{id}/audit")
  public List<Map<String,Object>> audit(@PathVariable Long id){
    List<Map<String,Object>> out = new ArrayList<>();
    for(var au : svc.audit(id)){
      var m = new LinkedHashMap<String,Object>();
      m.put("auditId", au.auditId); m.put("alertId", au.alertId);
      m.put("fromStatus", au.fromStatus); m.put("toStatus", au.toStatus);
      m.put("actor", Masking.maskSensitiveText(au.actor)); m.put("note", Masking.maskSensitiveText(au.note)); m.put("at", au.createdAt);
      out.add(m);
    }
    return out;
  }
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<?> conflict(ConflictException e){ return ResponseEntity.status(409).body(Map.of("error", e.getMessage())); }
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<?> notFound(NoSuchElementException e){ return ResponseEntity.status(404).body(Map.of("error", e.getMessage())); }
}
