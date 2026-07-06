package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.dto.response.AdminAuditLogResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public PageResponse<AdminAuditLogResponse> listAuditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return adminAuditService.listAuditLogs(page, size);
    }
}
