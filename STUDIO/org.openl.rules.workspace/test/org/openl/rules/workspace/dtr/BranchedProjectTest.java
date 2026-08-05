package org.openl.rules.workspace.dtr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.BranchedProject.BranchEntry;

class BranchedProjectTest {

    @Test
    void keepsTheActualBaseRefAsHomeAfterFiltering() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("main", entry(Instant.parse("2026-07-29T10:00:00Z")));
        entries.put("feature/rates", entry(Instant.parse("2026-07-29T11:00:00Z")));

        var project = BranchedProject.create("Rates", "MAIN", entries);
        var filtered = project.filter(ignored -> true).orElseThrow();

        assertEquals("main", project.homeBranch());
        assertEquals("main", filtered.homeBranch());
    }

    @Test
    void tellsWhetherOneBranchKeepsTheLastCopyOfTheProject() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("feature/rates", entry(Instant.parse("2026-07-29T11:00:00Z")));
        var alone = BranchedProject.create("Rates", "main", entries);

        assertTrue(alone.heldOnlyBy("feature/rates"));
        // The home branch is chosen without regard to case, and so is this.
        assertTrue(alone.heldOnlyBy("FEATURE/RATES"));
        assertFalse(alone.heldOnlyBy("main"), "A branch that does not hold the project holds no last copy.");

        entries.put("main", entry(Instant.parse("2026-07-29T10:00:00Z")));
        var shared = BranchedProject.create("Rates", "main", entries);

        assertFalse(shared.heldOnlyBy("feature/rates"));
    }

    private static BranchEntry entry(Instant lastCommitAt) {
        var status = new BranchStatus(new UserInfo("author"), lastCommitAt, "Change", "revision");
        return new BranchEntry(mock(AProject.class), status);
    }
}
