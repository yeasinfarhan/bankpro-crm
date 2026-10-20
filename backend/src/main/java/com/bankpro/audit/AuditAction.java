package com.bankpro.audit;

/** Audit event names. Extended as modules are added (e.g. LEAD_CONVERTED, CUSTOMER_ARCHIVED). */
public final class AuditAction {
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGIN_BLOCKED = "LOGIN_BLOCKED";
    public static final String CUSTOMER_CREATED = "CUSTOMER_CREATED";
    public static final String CUSTOMER_UPDATED = "CUSTOMER_UPDATED";
    public static final String CUSTOMER_ARCHIVED = "CUSTOMER_ARCHIVED";
    public static final String CUSTOMER_RESTORED = "CUSTOMER_RESTORED";
    public static final String CUSTOMERS_EXPORTED = "CUSTOMERS_EXPORTED";
    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String LEAD_CREATED = "LEAD_CREATED";
    public static final String LEAD_UPDATED = "LEAD_UPDATED";
    public static final String LEAD_STATUS_CHANGED = "LEAD_STATUS_CHANGED";
    public static final String LEAD_ASSIGNED = "LEAD_ASSIGNED";
    public static final String LEAD_CONVERTED = "LEAD_CONVERTED";
    public static final String SALE_DECIDED = "SALE_DECIDED";
    public static final String FOLLOW_UP_COMPLETED = "FOLLOW_UP_COMPLETED";

    private AuditAction() {
    }
}
