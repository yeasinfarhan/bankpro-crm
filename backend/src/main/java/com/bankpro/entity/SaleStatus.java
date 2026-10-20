package com.bankpro.entity;

/** COMPLETED counts as a sale. PENDING_APPROVAL (loans, cards) does not, until a manager records the simulated approval. */
public enum SaleStatus {
    COMPLETED, PENDING_APPROVAL, DECLINED
}
