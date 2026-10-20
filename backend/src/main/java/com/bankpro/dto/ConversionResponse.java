package com.bankpro.dto;

/**
 * @param alreadyConverted true when this request did not create a sale because the lead was converted earlier
 *                         (the existing sale is returned — repeated requests never create duplicates)
 */
public record ConversionResponse(LeadResponse lead, SaleResponse sale, boolean alreadyConverted) {
}
