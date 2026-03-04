package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.model.AuditLog;
import com.companyname.ragassistant.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLog> list(@RequestParam(defaultValue = "50") int limit) {
        return auditService.list(limit);
    }
}
