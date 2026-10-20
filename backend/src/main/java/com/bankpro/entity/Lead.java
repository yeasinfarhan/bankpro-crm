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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A sales opportunity: a customer's interest in one product, owned by one officer.
 * Status changes go through {@link com.bankpro.service.LeadStageRules}; the entity only stores the result.
 */
@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private BankingProduct product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_officer_id", nullable = false)
    private AppUser assignedOfficer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private LeadPriority priority = LeadPriority.MEDIUM;

    @Column(name = "estimated_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "loss_reason", length = 30)
    private LossReason lossReason;

    @Column(name = "loss_note", length = 500)
    private String lossNote;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

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

    protected Lead() {
    }

    public Lead(Customer customer, BankingProduct product, AppUser officer, AppUser createdBy, Instant now) {
        this.customer = customer;
        this.product = product;
        this.branch = customer.getBranch();
        this.assignedOfficer = officer;
        this.createdBy = createdBy;
        this.statusChangedAt = now;
    }

    public void updateDetails(LeadSource source, LeadPriority priority, BigDecimal estimatedValue,
                              LocalDate expectedCloseDate, String notes) {
        this.source = source;
        this.priority = priority;
        this.estimatedValue = estimatedValue;
        this.expectedCloseDate = expectedCloseDate;
        this.notes = notes;
    }

    public void changeProduct(BankingProduct product) {
        this.product = product;
    }

    /** Applies an already-validated stage change. */
    public void moveTo(LeadStatus next, Instant when, LossReason reason, String lossNote) {
        this.status = next;
        this.statusChangedAt = when;
        if (next == LeadStatus.LOST) {
            this.lossReason = reason;
            this.lossNote = lossNote;
        } else {
            this.lossReason = null;
            this.lossNote = null;
        }
        if (next != LeadStatus.NEW && lastContactedAt == null && next != LeadStatus.LOST) {
            this.lastContactedAt = when;
        }
    }

    public void assignTo(AppUser officer) {
        this.assignedOfficer = officer;
    }

    public void touchContact(Instant when) {
        if (lastContactedAt == null || when.isAfter(lastContactedAt)) {
            this.lastContactedAt = when;
        }
    }

    public boolean isOpen() {
        return status != LeadStatus.CONVERTED && status != LeadStatus.LOST;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public BankingProduct getProduct() { return product; }
    public Branch getBranch() { return branch; }
    public AppUser getAssignedOfficer() { return assignedOfficer; }
    public LeadSource getSource() { return source; }
    public LeadStatus getStatus() { return status; }
    public LeadPriority getPriority() { return priority; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public LossReason getLossReason() { return lossReason; }
    public String getLossNote() { return lossNote; }
    public String getNotes() { return notes; }
    public Instant getLastContactedAt() { return lastContactedAt; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
