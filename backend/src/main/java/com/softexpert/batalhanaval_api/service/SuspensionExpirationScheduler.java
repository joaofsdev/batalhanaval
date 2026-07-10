package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.domain.UserStatus;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuspensionExpirationScheduler {

    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void expireSuspensions() {
        List<User> expiredUsers = userRepository.findExpiredSuspensions(Instant.now());

        for (User user : expiredUsers) {
            user.setStatus(UserStatus.ACTIVE);
            user.setSuspendedUntil(null);
            userRepository.save(user);

            adminAuditService.log(null, "SUSPENSION_EXPIRED", "USER", user.getId(), null);

            log.info("Suspension expired: user={} ({})", user.getId(), user.getUsername());
        }

        if (!expiredUsers.isEmpty()) {
            log.info("Processed {} expired suspensions", expiredUsers.size());
        }
    }
}
