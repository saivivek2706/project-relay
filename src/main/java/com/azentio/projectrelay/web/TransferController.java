package com.azentio.projectrelay.web;

import com.azentio.projectrelay.dto.TransferRequest;
import com.azentio.projectrelay.service.TransferService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> ingest(@Valid @RequestBody TransferRequest request) {
        TransferService.Decision decision = service.ingest(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transferId", decision.transferId());
        body.put("decision", decision.decision());
        if (decision.alertId() != null) body.put("alertId", decision.alertId());
        return ResponseEntity.status(decision.duplicate() ? 200 : 201).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage(),
                        (first, ignored) -> first, LinkedHashMap::new));
        return ResponseEntity.badRequest().body(Map.of("errors", fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, Object>> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("errors",
                Map.of("request", ex.getMessage() == null ? "invalid request" : ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("errors",
                Map.of("request", "invalid JSON or occurredAt must be ISO-8601")));
    }
}
