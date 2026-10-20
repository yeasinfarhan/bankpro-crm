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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A goal for one officer (user set) or for the whole branch (user null) in one month/quarter.
 * The achieved value is NOT stored — it is always computed from sales, leads and customers (D-14).
 */
@Entity
@Table(name = "sales_targets")
public class SalesTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TargetCategory category;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "owner_key", insertable = false, updatable = false)
    private Long ownerKey;

    protected SalesTarget() {
    }

    public SalesTarget(Branch branch, AppUser user, PeriodType type, LocalDate start, LocalDate end,
                       TargetCategory category, BigDecimal value, AppUser createdBy) {
        this.branch = branch;
        this.user = user;
        this.periodType = type;
        this.periodStart = start;
        this.periodEnd = end;
        this.category = category;
        this.targetValue = value;
        this.createdBy = createdBy;
    }

    public void changeValue(BigDecimal value) {
        this.targetValue = value;
    }

    public Long getId() { return id; }
    public Branch getBranch() { return branch; }
    public AppUser getUser() { return user; }
    public PeriodType getPeriodType() { return periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public TargetCategory getCategory() { return category; }
    public BigDecimal getTargetValue() { return targetValue; }
    public Instant getCreatedAt() { return createdAt; }
}
