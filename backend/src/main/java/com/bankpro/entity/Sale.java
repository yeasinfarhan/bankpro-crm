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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A SIMULATED sale created by converting a lead. No money moves and no real account is opened.
 * {@code UNIQUE(lead_id)} in the database guarantees at most one sale per lead.
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    private Lead lead;

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
    @JoinColumn(name = "officer_id", nullable = false)
    private AppUser officer;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(name = "converted_at", nullable = false)
    private Instant convertedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private AppUser decidedBy;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    protected Sale() {
    }

    public Sale(Lead lead, BigDecimal amount, SaleStatus status, Instant convertedAt) {
        this.lead = lead;
        this.customer = lead.getCustomer();
        this.product = lead.getProduct();
        this.branch = lead.getBranch();
        this.officer = lead.getAssignedOfficer();
        this.amount = amount;
        this.status = status;
        this.convertedAt = convertedAt;
    }

    public void decide(SaleStatus outcome, AppUser manager, Instant when, String note) {
        this.status = outcome;
        this.decidedBy = manager;
        this.decidedAt = when;
        this.decisionNote = note;
    }

    public Long getId() { return id; }
    public Lead getLead() { return lead; }
    public Customer getCustomer() { return customer; }
    public BankingProduct getProduct() { return product; }
    public Branch getBranch() { return branch; }
    public AppUser getOfficer() { return officer; }
    public BigDecimal getAmount() { return amount; }
    public SaleStatus getStatus() { return status; }
    public Instant getConvertedAt() { return convertedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public AppUser getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
}
