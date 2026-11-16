package com.azentio.projectrelay.web;
import com.azentio.projectrelay.dto.TransferRequest;import com.azentio.projectrelay.service.TransferService;import jakarta.validation.Valid;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api") public class TransferController{
 private final TransferService svc; public TransferController(TransferService s){svc=s;}
 @PostMapping("/transfers") public ResponseEntity<?> ingest(@Valid @RequestBody TransferRequest req){
   var d=svc.ingest(req); var body=new java.util.LinkedHashMap<String,Object>(); body.put("transferId",d.transferId()); body.put("decision",d.decision()); if(d.alertId()!=null) body.put("alertId",d.alertId());
   return ResponseEntity.status(d.duplicate() ? 200 : 201).body(body);
 }
 @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MethodArgumentNotValidException.class, jakarta.validation.ConstraintViolationException.class}) public ResponseEntity<?> bad(Exception e){ return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()==null?"invalid input":e.getMessage())); }
}