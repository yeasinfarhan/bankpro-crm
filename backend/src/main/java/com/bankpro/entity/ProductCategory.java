package com.bankpro.entity;

/** Product families. DEPOSIT = FDR/DPS (counts as deposit sales); LOAN and CARD conversions need approval. */
public enum ProductCategory {
    ACCOUNT, DEPOSIT, CARD, LOAN, SME
}
