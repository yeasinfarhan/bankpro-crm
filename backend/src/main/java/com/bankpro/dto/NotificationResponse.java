package com.bankpro.dto;

import java.time.Instant;
import java.util.List;

public record NotificationResponse(Long id, String type, String message, String link, boolean read, Instant createdAt) {

    public record Inbox(long unreadCount, List<NotificationResponse> items) {
    }
}
