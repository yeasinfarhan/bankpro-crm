package com.bankpro.dto;

import com.bankpro.entity.CustomerSegment;
import com.bankpro.entity.CustomerStatus;
import com.bankpro.entity.EmploymentType;
import com.bankpro.entity.IncomeRange;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create/update body for a customer. Phone and email are normalised in the service before duplicate checks.
 * Only the minimum information a branch CRM needs — no account numbers, card numbers, NID or date of birth.
 *
 * @param assignedOfficerId managers may assign any active officer in their branch; officers always own
 *                          the customers they create (the value is ignored for them)
 * @param productIds        products the customer already holds (manually recorded relationships)
 * @param status            PROSPECT, ACTIVE or INACTIVE (archiving uses DELETE)
 * @param version           required on update for optimistic locking
 */
public record CustomerRequest(
        @NotBlank(message = "Full name is required") @Size(min = 2, max = 120, message = "Name must be 2–120 characters")
        String fullName,

        @Email(message = "Enter a valid email address") @Size(max = 190, message = "Email is too long")
        String email,

        @NotBlank(message = "Phone number is required") @Size(max = 25, message = "Phone number is too long")
        String phone,

        @Size(max = 80, message = "Occupation is too long")
        String occupation,

        EmploymentType employmentType,
        IncomeRange incomeRange,

        @Size(max = 80, message = "City is too long")
        String city,

        @NotNull(message = "Segment is required")
        CustomerSegment segment,

        CustomerStatus status,
        Long assignedOfficerId,

        @Size(max = 20, message = "Too many products")
        List<Long> productIds,

        Long version
) {
    public CustomerRequest {
        email = email == null || email.isBlank() ? null : email.trim();
    }
}
