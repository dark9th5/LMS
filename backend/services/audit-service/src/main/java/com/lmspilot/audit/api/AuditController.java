package com.lmspilot.audit.api;

import com.lmspilot.contracts.Permissions;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/audit")
public class AuditController {
 private final AuditService service; public AuditController(AuditService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAuthority('"+Permissions.AUDIT_READ+"')")
 public Page<AuditEntryResponse> search(@RequestParam(required=false)String actor,@RequestParam(required=false)String action,@RequestParam(required=false)String resourceType,
 @RequestParam(required=false)String resourceId,@RequestParam(required=false)String outcome,@RequestParam(required=false)String correlationId,
 @RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.search(actor,action,resourceType,resourceId,outcome,correlationId,from,to,page,size);}
 @GetMapping("/export.csv") @PreAuthorize("hasAuthority('"+Permissions.AUDIT_EXPORT+"')")
 public ResponseEntity<byte[]> export(@RequestParam(required=false)String actor,@RequestParam(required=false)String action,@RequestParam(required=false)String resourceType,
 @RequestParam(required=false)String resourceId,@RequestParam(required=false)String outcome,@RequestParam(required=false)String correlationId,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to){
 return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=audit-export.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.export(actor,action,resourceType,resourceId,outcome,correlationId,from,to));}
}
