package org.openl.rules.workspace.dtr;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static BranchEntry entry(Instant lastCommitAt) {
        var status = new BranchStatus(new UserInfo("author"), lastCommitAt, "Change", "revision");
        return new BranchEntry(mock(AProject.class), status);
    }
}
