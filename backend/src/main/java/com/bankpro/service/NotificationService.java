package com.bankpro.service;

import com.bankpro.dto.NotificationResponse;
import com.bankpro.entity.Notification;
import com.bankpro.exception.NotFoundException;
import com.bankpro.repository.AppUserRepository;
import com.bankpro.repository.NotificationRepository;
import com.bankpro.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** In-app notifications. Every user only ever sees and changes their own. */
@Service
@Transactional
public class NotificationService {

    public static final String LEAD_ASSIGNED = "LEAD_ASSIGNED";
    public static final String FOLLOW_UP_OVERDUE = "FOLLOW_UP_OVERDUE";
    public static final String SALE_DECIDED = "SALE_DECIDED";

    private final NotificationRepository notifications;
    private final AppUserRepository users;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, AppUserRepository users, Clock clock) {
        this.notifications = notifications;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Creates a notification. With a {@code dedupeKey}, the same reminder is never created twice for a user
     * (also guaranteed by UNIQUE(user_id, dedupe_key) in the database).
     */
    public void notify(Long userId, String type, String message, String link, String dedupeKey) {
        if (dedupeKey != null && notifications.existsByUserIdAndDedupeKey(userId, dedupeKey)) {
            return;
        }
        notifications.save(new Notification(users.getReferenceById(userId), type, message, link, dedupeKey,
                clock.instant()));
    }

    @Transactional(readOnly = true)
    public NotificationResponse.Inbox inbox(AuthenticatedUser caller, int limit) {
        var items = notifications.findByUserIdOrderByCreatedAtDescIdDesc(caller.id(),
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 50))).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getLink(), n.isRead(),
                        n.getCreatedAt()))
                .toList();
        return new NotificationResponse.Inbox(notifications.countByUserIdAndReadFalse(caller.id()), items);
    }

    public void markRead(AuthenticatedUser caller, Long id) {
        notifications.findByIdAndUserId(id, caller.id())
                .orElseThrow(() -> new NotFoundException("Notification not found."))
                .markRead();
    }

    public int markAllRead(AuthenticatedUser caller) {
        return notifications.markAllRead(caller.id());
    }
}
