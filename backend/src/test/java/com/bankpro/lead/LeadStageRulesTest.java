package com.bankpro.lead;

import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.Role;
import com.bankpro.service.LeadStageRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.bankpro.service.LeadStageRules.Verdict.*;
import static org.assertj.core.api.Assertions.assertThat;

class LeadStageRulesTest {

    @ParameterizedTest(name = "{0} -> {1} as {2} = {3}")
    @CsvSource({
            "NEW, CONTACTED, RELATIONSHIP_OFFICER, ALLOWED",
            "CONTACTED, QUALIFIED, RELATIONSHIP_OFFICER, ALLOWED",
            "QUALIFIED, PROPOSAL, RELATIONSHIP_OFFICER, ALLOWED",
            "CONTACTED, NEW, RELATIONSHIP_OFFICER, ALLOWED",
            "PROPOSAL, QUALIFIED, RELATIONSHIP_OFFICER, ALLOWED",
            "NEW, QUALIFIED, RELATIONSHIP_OFFICER, NOT_ALLOWED",
            "NEW, PROPOSAL, BRANCH_MANAGER, NOT_ALLOWED",
            "PROPOSAL, CONTACTED, BRANCH_MANAGER, NOT_ALLOWED",
            "NEW, LOST, RELATIONSHIP_OFFICER, ALLOWED",
            "PROPOSAL, LOST, RELATIONSHIP_OFFICER, ALLOWED",
            "QUALIFIED, CONVERTED, BRANCH_MANAGER, USE_CONVERT",
            "CONVERTED, LOST, BRANCH_MANAGER, FINAL",
            "LOST, CONTACTED, RELATIONSHIP_OFFICER, MANAGER_ONLY",
            "LOST, CONTACTED, BRANCH_MANAGER, ALLOWED",
            "LOST, QUALIFIED, BRANCH_MANAGER, NOT_ALLOWED",
            "QUALIFIED, QUALIFIED, RELATIONSHIP_OFFICER, NO_CHANGE"
    })
    void transitions(LeadStatus from, LeadStatus to, Role role, LeadStageRules.Verdict expected) {
        assertThat(LeadStageRules.check(from, to, role)).isEqualTo(expected);
    }

    @Test
    void nextMovesForTheUi() {
        assertThat(LeadStageRules.nextFor(LeadStatus.CONTACTED, Role.RELATIONSHIP_OFFICER))
                .containsExactlyInAnyOrder(LeadStatus.NEW, LeadStatus.QUALIFIED, LeadStatus.LOST);
        assertThat(LeadStageRules.nextFor(LeadStatus.LOST, Role.RELATIONSHIP_OFFICER)).isEmpty();
        assertThat(LeadStageRules.nextFor(LeadStatus.LOST, Role.BRANCH_MANAGER)).containsExactly(LeadStatus.CONTACTED);
        assertThat(LeadStageRules.nextFor(LeadStatus.CONVERTED, Role.BRANCH_MANAGER)).isEmpty();
    }
}
