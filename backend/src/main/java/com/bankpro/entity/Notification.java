package com.bankpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** In-app notification for one user (no email/SMS in the MVP). */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(length = 200)
    private String link;

    @Column(name = "dedupe_key", length = 100)
    private String dedupeKey;

    @Column(name = "read_flag", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(AppUser user, String type, String message, String link, String dedupeKey, Instant createdAt) {
        this.user = user;
        this.type = type;
        this.message = message.length() > 300 ? message.substring(0, 297) + "..." : message;
        this.link = link;
        this.dedupeKey = dedupeKey;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public String getDedupeKey() { return dedupeKey; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
