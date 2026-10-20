package com.bankpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A fictional product in the catalogue. Rates, if any, are illustrative sample values only. */
@Entity
@Table(name = "banking_products")
public class BankingProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "eligibility_notes", columnDefinition = "text")
    private String eligibilityNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> benefits = new ArrayList<>();

    @Column(name = "illustrative_rate", precision = 5, scale = 2)
    private BigDecimal illustrativeRate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankingProduct() {
    }

    public BankingProduct(String code) {
        this.code = code;
    }

    /** Applies editable fields in one place so create and update share the same rules. */
    public void update(String name, ProductCategory category, String description, String eligibilityNotes,
                       List<String> benefits, BigDecimal illustrativeRate, boolean active) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.eligibilityNotes = eligibilityNotes;
        this.benefits = benefits == null ? new ArrayList<>() : new ArrayList<>(benefits);
        this.illustrativeRate = illustrativeRate;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getEligibilityNotes() { return eligibilityNotes; }
    public List<String> getBenefits() { return benefits; }
    public BigDecimal getIllustrativeRate() { return illustrativeRate; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
