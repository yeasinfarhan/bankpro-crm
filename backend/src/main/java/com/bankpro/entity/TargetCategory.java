package com.bankpro.entity;

/** What a sales target measures. DEPOSIT_SALES is in BDT; the others are counts. */
public enum TargetCategory {
    NEW_CUSTOMERS, DEPOSIT_SALES, CREDIT_CARD_SALES, LOAN_LEADS, CONVERTED_LEADS;

    public boolean isMoney() {
        return this == DEPOSIT_SALES;
    }
}
