package com.bankpro.entity;

/** Pipeline stages. CONVERTED is only reachable through the convert action (which creates the sale). */
public enum LeadStatus {
    NEW, CONTACTED, QUALIFIED, PROPOSAL, CONVERTED, LOST
}
