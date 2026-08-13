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
    void representsAProjectOutsideTheBaseBranchByAProtectedBranch() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("release-2024.1", entry(Instant.parse("2026-07-29T10:00:00Z"), true));
        entries.put("EPBDS-12345-fix", entry(Instant.parse("2026-07-29T11:00:00Z"), false));

        var project = BranchedProject.create("Rates", "main", entries);

        assertEquals("release-2024.1", project.homeBranch(),
                "A protected branch must outrank a ticket branch that was pushed to later.");
        assertEquals("EPBDS-12345-fix",
                project.filter(entry -> !entry.status().protectedBranch()).orElseThrow().homeBranch(),
                "A caller who cannot read the protected branch must be given the next branch by the same rule.");
    }

    @Test
    void representsAProjectByTheNewestBranchWhenNoBranchIsProtected() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("EPBDS-1-fix", entry(Instant.parse("2026-07-29T10:00:00Z"), false));
        entries.put("EPBDS-2-fix", entry(Instant.parse("2026-07-29T11:00:00Z"), false));

        assertEquals("EPBDS-2-fix", BranchedProject.create("Rates", "main", entries).homeBranch());
    }

    @Test
    void tellsWhetherOneBranchKeepsTheLastCopyOfTheProject() {
        var entries = new LinkedHashMap<String, BranchEntry>();
        entries.put("feature/rates", entry(Instant.parse("2026-07-29T11:00:00Z")));
        var alone = BranchedProject.create("Rates", "main", entries);

        assertTrue(alone.heldOnlyBy("feature/rates"));
        // Git tells apart branches differing only in case, so a look-alike name is a different branch.
        assertFalse(alone.heldOnlyBy("FEATURE/RATES"));
        assertFalse(alone.heldOnlyBy("main"), "A branch that does not hold the project holds no last copy.");

        entries.put("main", entry(Instant.parse("2026-07-29T10:00:00Z")));
        var shared = BranchedProject.create("Rates", "main", entries);

        assertFalse(shared.heldOnlyBy("feature/rates"));
    }

    private static BranchEntry entry(Instant lastCommitAt) {
        return entry(lastCommitAt, false);
    }

    private static BranchEntry entry(Instant lastCommitAt, boolean protectedBranch) {
        var status = new BranchStatus(new UserInfo("author"), lastCommitAt, "Change", "revision", protectedBranch);
        return new BranchEntry(mock(AProject.class), status);
    }
}
