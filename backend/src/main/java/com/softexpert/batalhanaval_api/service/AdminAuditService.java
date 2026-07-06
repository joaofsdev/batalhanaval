package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.AdminAuditLog;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.AdminAuditLogResponse;
import com.softexpert.batalhanaval_api.dto.response.PageResponse;
import com.softexpert.batalhanaval_api.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public void log(User admin, String action, String targetType, UUID targetId, String details) {
        AdminAuditLog entry = new AdminAuditLog();
        entry.setAdmin(admin);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> listAuditLogs(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AdminAuditLog> logPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);

        var content = logPage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new PageResponse<>(content, logPage.getNumber(), logPage.getSize(),
            logPage.getTotalElements(), logPage.getTotalPages());
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log) {
        return new AdminAuditLogResponse(
            log.getId(),
            log.getAdmin() != null ? log.getAdmin().getId() : null,
            log.getAdmin() != null ? log.getAdmin().getUsername() : "SYSTEM",
            log.getAction(),
            log.getTargetType(),
            log.getTargetId(),
            log.getDetails(),
            log.getCreatedAt()
        );
    }
}
