package com.bankpro.audit;

import com.bankpro.entity.AuditLog;
import com.bankpro.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/**
 * Writes audit records.
 * {@code REQUIRES_NEW} runs the insert in its own transaction, so the record is kept even when the
 * surrounding operation fails and rolls back (e.g. a failed login still leaves a LOGIN_FAILED entry).
 * Callers must not put passwords, tokens or unnecessary customer data in {@code details}.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String action, String entityType, Long entityId,
                       Map<String, Object> details, String ipAddress) {
        repository.save(new AuditLog(actorId, action, entityType, entityId, details, ipAddress, clock.instant()));
    }

    /** Same as above, taking the client IP from the current HTTP request (if any). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String action, String entityType, Long entityId, Map<String, Object> details) {
        record(actorId, action, entityType, entityId, details, currentIp());
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
