package com.bankpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** What actually happened with a customer (a completed follow-up or an ad-hoc note). Append-only history. */
@Entity
@Table(name = "customer_interactions")
public class CustomerInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_id")
    private FollowUpActivity followUp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private AppUser recordedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "interaction_type", nullable = false, length = 30)
    private InteractionType interactionType;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerInteraction() {
    }

    public CustomerInteraction(Customer customer, Lead lead, FollowUpActivity followUp, AppUser recordedBy,
                               InteractionType type, String summary, Instant occurredAt) {
        this.customer = customer;
        this.lead = lead;
        this.followUp = followUp;
        this.recordedBy = recordedBy;
        this.interactionType = type;
        this.summary = summary;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Lead getLead() { return lead; }
    public FollowUpActivity getFollowUp() { return followUp; }
    public AppUser getRecordedBy() { return recordedBy; }
    public InteractionType getInteractionType() { return interactionType; }
    public String getSummary() { return summary; }
    public Instant getOccurredAt() { return occurredAt; }
}
