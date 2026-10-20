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

/**
 * A (prospective) customer of the fictional bank. Synthetic data only.
 * Deliberately has NO account numbers, card numbers, national ID or date of birth.
 * {@code @Version} enables optimistic locking: if two officers edit the same customer, the second save
 * fails with 409 instead of silently overwriting the first.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 190)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 80)
    private String occupation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "income_range", length = 30)
    private IncomeRange incomeRange;

    @Column(length = 80)
    private String city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private CustomerSegment segment;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private AppUser assignedOfficer;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

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

    protected Customer() {
    }

    public Customer(Branch branch, AppUser createdBy) {
        this.branch = branch;
        this.createdBy = createdBy;
    }

    public void updateProfile(String fullName, String email, String phone, String occupation,
                              EmploymentType employmentType, IncomeRange incomeRange, String city,
                              CustomerSegment segment) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.occupation = occupation;
        this.employmentType = employmentType;
        this.incomeRange = incomeRange;
        this.city = city;
        this.segment = segment;
    }

    public void changeStatus(CustomerStatus newStatus) {
        this.status = newStatus;
    }

    public void assignTo(AppUser officer) {
        this.assignedOfficer = officer;
    }

    public void archive(Instant when) {
        this.status = CustomerStatus.ARCHIVED;
        this.archivedAt = when;
    }

    public void restore() {
        this.status = CustomerStatus.PROSPECT;
        this.archivedAt = null;
    }

    public void touchInteraction(Instant when) {
        if (lastInteractionAt == null || when.isAfter(lastInteractionAt)) {
            this.lastInteractionAt = when;
        }
    }

    public boolean isArchived() {
        return status == CustomerStatus.ARCHIVED;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getOccupation() { return occupation; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public IncomeRange getIncomeRange() { return incomeRange; }
    public String getCity() { return city; }
    public CustomerSegment getSegment() { return segment; }
    public CustomerStatus getStatus() { return status; }
    public Branch getBranch() { return branch; }
    public AppUser getAssignedOfficer() { return assignedOfficer; }
    public Instant getLastInteractionAt() { return lastInteractionAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
