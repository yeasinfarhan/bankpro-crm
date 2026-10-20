package com.bankpro.security;

import com.bankpro.entity.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring, no database. */
class DataScopeTest {

    private static final long BRANCH_A = 1L;
    private static final long BRANCH_B = 2L;

    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "a@x", "Admin", Role.ADMIN, null);
    private final AuthenticatedUser managerA = new AuthenticatedUser(2L, "m@x", "Manager", Role.BRANCH_MANAGER, BRANCH_A);
    private final AuthenticatedUser officerA = new AuthenticatedUser(3L, "o@x", "Officer", Role.RELATIONSHIP_OFFICER, BRANCH_A);

    @Test
    void adminSeesEverything() {
        DataScope scope = DataScope.of(admin);
        assertThat(scope.level()).isEqualTo(DataScope.Level.ALL);
        assertThat(scope.allows(BRANCH_B, 99L)).isTrue();
    }

    @Test
    void managerSeesOwnBranchOnly() {
        DataScope scope = DataScope.of(managerA);
        assertThat(scope.allows(BRANCH_A, 99L)).isTrue();
        assertThat(scope.allows(BRANCH_B, 2L)).isFalse();
    }

    @Test
    void officerSeesOnlyOwnRecordsInOwnBranch() {
        DataScope scope = DataScope.of(officerA);
        assertThat(scope.allows(BRANCH_A, 3L)).isTrue();
        assertThat(scope.allows(BRANCH_A, 4L)).as("colleague's record").isFalse();
        assertThat(scope.allows(BRANCH_B, 3L)).as("other branch").isFalse();
    }

    @Test
    void officerBranchReadCoversWholeBranch() {
        DataScope scope = DataScope.branchRead(officerA);
        assertThat(scope.allows(BRANCH_A, 4L)).isTrue();
        assertThat(scope.allows(BRANCH_B, 4L)).isFalse();
    }

    @Test
    void nonAdminWithoutBranchIsRejected() {
        AuthenticatedUser broken = new AuthenticatedUser(9L, "b@x", "Broken", Role.BRANCH_MANAGER, null);
        assertThatThrownBy(() -> DataScope.of(broken)).isInstanceOf(IllegalStateException.class);
    }
}
