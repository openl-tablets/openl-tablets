package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.openl.rules.workspace.StaleIds.Deleted;
import org.openl.rules.workspace.StaleIds.Moved;

class StaleProjectIdsTest {

    private static final String DESIGN = "design";
    private static final ProjectId BANK = ProjectIds.of(DESIGN, "rules/Bank");
    private static final ProjectId BANK_MOVED = ProjectIds.of(DESIGN, "rules/archive/Bank");
    private static final ProjectId BANK_MOVED_AGAIN = ProjectIds.of(DESIGN, "rules/2026/Bank");
    private static final ProjectId AUTO = ProjectIds.of(DESIGN, "rules/Auto");

    private final StaleProjectIds ledger = new StaleProjectIds();

    @Test
    void theFirstListingOfARepositoryTeachesNothing() {
        ledger.listed(DESIGN, Map.of(BANK_MOVED, "Bank"));

        assertTrue(ledger.changeOf(BANK).isEmpty());
        assertTrue(ledger.changeOf(BANK_MOVED).isEmpty());
    }

    @Test
    void aProjectNoListingShowsAnyMoreWasDeleted() {
        ledger.listed(DESIGN, Map.of(BANK, "Bank", AUTO, "Auto"));
        ledger.listed(DESIGN, Map.of(AUTO, "Auto"));

        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(BANK));
        assertTrue(ledger.changeOf(AUTO).isEmpty());
    }

    @Test
    void aProjectOfTheSameNameInANewFolderIsTheProjectMoved() {
        ledger.listed(DESIGN, Map.of(BANK, "Bank", AUTO, "Auto"));
        // Names are compared regardless of case, as project names are.
        ledger.listed(DESIGN, Map.of(BANK_MOVED, "BANK", AUTO, "Auto"));

        assertEquals(Optional.of(new Moved<>(BANK_MOVED)), ledger.changeOf(BANK));
        assertTrue(ledger.changeOf(BANK_MOVED).isEmpty());
    }

    @Test
    void aNewFolderOfAnotherNameIsAnotherProject() {
        ledger.listed(DESIGN, Map.of(BANK, "Bank"));
        ledger.listed(DESIGN, Map.of(BANK_MOVED, "Bank Rating"));

        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(BANK));
    }

    @Test
    void severalFoldersOfOneNameTellNoMove() {
        var twin = ProjectIds.of(DESIGN, "rules/twin/Bank");
        ledger.listed(DESIGN, Map.of(BANK, "Bank", twin, "Bank"));
        ledger.listed(DESIGN, Map.of(BANK_MOVED, "Bank"));

        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(BANK));
        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(twin));

        ledger.listed(DESIGN, Map.of(BANK_MOVED_AGAIN, "Bank", AUTO, "Bank"));

        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(BANK_MOVED));
    }

    @Test
    void aProjectCreatedAgainInTheFolderTakesTheIdOutOfTheLedger() {
        ledger.listed(DESIGN, Map.of(BANK, "Bank"));
        ledger.listed(DESIGN, Map.of());
        ledger.listed(DESIGN, Map.of(BANK, "Bank Rating"));

        assertTrue(ledger.changeOf(BANK).isEmpty());
    }

    @Test
    void eachRepositoryIsComparedWithItsOwnListing() {
        var other = ProjectIds.of("other", "rules/Bank");
        ledger.listed(DESIGN, Map.of(BANK, "Bank"));
        ledger.listed("other", Map.of(other, "Bank"));
        ledger.listed("other", Map.of(other, "Bank"));

        assertTrue(ledger.changeOf(BANK).isEmpty());

        ledger.listed(DESIGN, Map.of());

        assertEquals(Optional.of(new Deleted<>()), ledger.changeOf(BANK));
        assertTrue(ledger.changeOf(other).isEmpty());
    }
}
