package com.bankpro.service;

import com.bankpro.entity.FollowUpActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Every 5 minutes: notify officers about follow-ups that have become overdue.
 * The dedupe key includes the scheduled time, so each overdue occurrence is reported once
 * (and again only if the activity is rescheduled and becomes overdue a second time).
 */
@Component
public class OverdueFollowUpNotifier {

    private static final Logger log = LoggerFactory.getLogger(OverdueFollowUpNotifier.class);
    private static final int BATCH = 500;

    private final com.bankpro.repository.FollowUpRepository followUps;
    private final NotificationService notifications;
    private final Clock clock;

    public OverdueFollowUpNotifier(com.bankpro.repository.FollowUpRepository followUps,
                                   NotificationService notifications, Clock clock) {
        this.followUps = followUps;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public int notifyOverdue() {
        int sent = 0;
        for (FollowUpActivity f : followUps.findOverdue(clock.instant(), PageRequest.of(0, BATCH))) {
            String key = "FOLLOW_UP_OVERDUE:" + f.getId() + ":" + f.getScheduledAt().getEpochSecond();
            notifications.notify(f.getAssignedOfficer().getId(), NotificationService.FOLLOW_UP_OVERDUE,
                    "Overdue follow-up: " + InteractionService.label(f.getActivityType().name()) + " with "
                            + f.getCustomer().getFullName(),
                    "/follow-ups?view=OVERDUE", key);
            sent++;
        }
        return sent;
    }

    /** Scheduling is switched on by app.scheduling.enabled (true by default, off in tests). */
    @Component
    @ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    static class Job {
        private final OverdueFollowUpNotifier notifier;

        Job(OverdueFollowUpNotifier notifier) {
            this.notifier = notifier;
        }

        @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT5M")
        void run() {
            try {
                notifier.notifyOverdue();
            } catch (RuntimeException e) {
                log.warn("Overdue follow-up check failed: {}", e.getMessage());
            }
        }
    }
}
