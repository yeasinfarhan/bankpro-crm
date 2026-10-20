package com.bankpro.service;

import com.bankpro.entity.LeadStatus;
import com.bankpro.entity.Role;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.bankpro.entity.LeadStatus.CONTACTED;
import static com.bankpro.entity.LeadStatus.CONVERTED;
import static com.bankpro.entity.LeadStatus.LOST;
import static com.bankpro.entity.LeadStatus.NEW;
import static com.bankpro.entity.LeadStatus.PROPOSAL;
import static com.bankpro.entity.LeadStatus.QUALIFIED;

/**
 * The sales pipeline rules in one place (pure Java, no database — easy to unit test).
 * <pre>
 *   NEW ⇄ CONTACTED ⇄ QUALIFIED ⇄ PROPOSAL      forward one step, or back one step to correct a mistake
 *   any open stage → LOST                        a loss reason is required
 *   LOST → CONTACTED                             reopen, branch managers only
 *   QUALIFIED / PROPOSAL → CONVERTED             only via the convert action (it creates the sale)
 *   CONVERTED                                    final
 * </pre>
 */
public final class LeadStageRules {

    public static final Set<LeadStatus> OPEN = EnumSet.of(NEW, CONTACTED, QUALIFIED, PROPOSAL);
    public static final Set<LeadStatus> CONVERTIBLE = EnumSet.of(QUALIFIED, PROPOSAL);

    private static final Map<LeadStatus, Set<LeadStatus>> MOVES = Map.of(
            NEW, EnumSet.of(CONTACTED, LOST),
            CONTACTED, EnumSet.of(NEW, QUALIFIED, LOST),
            QUALIFIED, EnumSet.of(CONTACTED, PROPOSAL, LOST),
            PROPOSAL, EnumSet.of(QUALIFIED, LOST),
            LOST, EnumSet.of(CONTACTED),
            CONVERTED, EnumSet.noneOf(LeadStatus.class));

    private LeadStageRules() {
    }

    public enum Verdict { ALLOWED, NO_CHANGE, USE_CONVERT, FINAL, MANAGER_ONLY, NOT_ALLOWED }

    public static Verdict check(LeadStatus from, LeadStatus to, Role role) {
        if (from == to) {
            return Verdict.NO_CHANGE;
        }
        if (to == CONVERTED) {
            return Verdict.USE_CONVERT;
        }
        if (from == CONVERTED) {
            return Verdict.FINAL;
        }
        if (!MOVES.get(from).contains(to)) {
            return Verdict.NOT_ALLOWED;
        }
        if (from == LOST && role != Role.BRANCH_MANAGER) {
            return Verdict.MANAGER_ONLY;
        }
        return Verdict.ALLOWED;
    }

    /** Status moves the given role may make next (for the UI). Excludes CONVERTED, which has its own action. */
    public static Set<LeadStatus> nextFor(LeadStatus from, Role role) {
        Set<LeadStatus> result = EnumSet.noneOf(LeadStatus.class);
        for (LeadStatus to : MOVES.get(from)) {
            if (check(from, to, role) == Verdict.ALLOWED) {
                result.add(to);
            }
        }
        return result;
    }
}
