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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A product the customer already holds. Recorded manually by an officer (sourceSaleId = null)
 * or created automatically when a converted sale is completed.
 */
@Entity
@Table(name = "customer_products")
public class CustomerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private BankingProduct product;

    @Column(name = "since_date", nullable = false)
    private LocalDate sinceDate;

    @Column(name = "source_sale_id")
    private Long sourceSaleId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomerProduct() {
    }

    public CustomerProduct(Customer customer, BankingProduct product, LocalDate sinceDate, Long sourceSaleId) {
        this.customer = customer;
        this.product = product;
        this.sinceDate = sinceDate;
        this.sourceSaleId = sourceSaleId;
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public BankingProduct getProduct() { return product; }
    public LocalDate getSinceDate() { return sinceDate; }
    public Long getSourceSaleId() { return sourceSaleId; }
    public boolean isManual() { return sourceSaleId == null; }
}
