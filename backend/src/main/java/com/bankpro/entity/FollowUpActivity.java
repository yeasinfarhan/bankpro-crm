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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** A scheduled activity (call, meeting, ...) with a customer, optionally about a specific lead. */
@Entity
@Table(name = "follow_up_activities")
public class FollowUpActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_officer_id", nullable = false)
    private AppUser assignedOfficer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status = FollowUpStatus.SCHEDULED;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(length = 500)
    private String outcome;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected FollowUpActivity() {
    }

    public FollowUpActivity(Customer customer, Lead lead, AppUser officer, AppUser createdBy, ActivityType type,
                            Instant scheduledAt, String notes) {
        this.customer = customer;
        this.lead = lead;
        this.branch = customer.getBranch();
        this.assignedOfficer = officer;
        this.createdBy = createdBy;
        this.activityType = type;
        this.scheduledAt = scheduledAt;
        this.notes = notes;
    }

    public void complete(String outcome, String notes, Instant when) {
        this.status = FollowUpStatus.COMPLETED;
        this.outcome = outcome;
        if (notes != null) {
            this.notes = notes;
        }
        this.completedAt = when;
    }

    public void reschedule(Instant when) {
        this.scheduledAt = when;
    }

    public boolean isOverdue(Instant now) {
        return status == FollowUpStatus.SCHEDULED && scheduledAt.isBefore(now);
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Lead getLead() { return lead; }
    public Branch getBranch() { return branch; }
    public AppUser getAssignedOfficer() { return assignedOfficer; }
    public ActivityType getActivityType() { return activityType; }
    public Instant getScheduledAt() { return scheduledAt; }
    public FollowUpStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getOutcome() { return outcome; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
