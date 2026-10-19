package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.openl.rules.workspace.StaleIds.Deleted;
import org.openl.rules.workspace.StaleIds.Moved;

class StaleIdsTest {

    private final StaleIds<String> ids = new StaleIds<>(1000);

    @Test
    void anIdNoChangeIsKnownOfNamesNoChange() {
        assertTrue(ids.changeOf("a").isEmpty());
    }

    @Test
    void aChainOfChangesLeadsToTheIdAtItsEnd() {
        ids.moved("a", "b");
        ids.moved("b", "c");

        assertEquals(Optional.of(new Moved<>("c")), ids.changeOf("a"));
        assertEquals(Optional.of(new Moved<>("c")), ids.changeOf("b"));
        assertTrue(ids.changeOf("c").isEmpty());

        ids.deleted("c");

        assertEquals(Optional.of(new Deleted<>()), ids.changeOf("a"));
        assertEquals(Optional.of(new Deleted<>()), ids.changeOf("b"));
        assertEquals(Optional.of(new Deleted<>()), ids.changeOf("c"));
    }

    @Test
    void anIdMovedBackNamesNoChange() {
        ids.moved("a", "b");
        ids.moved("b", "a");

        assertTrue(ids.changeOf("a").isEmpty());
        assertEquals(Optional.of(new Moved<>("a")), ids.changeOf("b"));
    }

    @Test
    void anIdMovedToItselfNamesNoChange() {
        ids.moved("a", "a");

        assertTrue(ids.changeOf("a").isEmpty());
    }

    @Test
    void anIdTakenAgainNamesNoChange() {
        ids.moved("a", "b");
        ids.deleted("b");
        ids.taken("b");

        assertTrue(ids.changeOf("b").isEmpty());
        assertEquals(Optional.of(new Deleted<>()), ids.changeOf("a"));
    }

    @Test
    void anIdOfSomethingDeletedThatChangesAgainWasTakenAgain() {
        ids.deleted("a");
        ids.moved("a", "b");

        assertEquals(Optional.of(new Moved<>("b")), ids.changeOf("a"));
    }

    @Test
    void aChangeFoundByComparingLeavesAnIdOfSomethingDeletedDeleted() {
        ids.deleted("a");
        ids.movedUnlessDeleted("a", "b");
        ids.movedUnlessDeleted("c", "d");

        assertEquals(Optional.of(new Deleted<>()), ids.changeOf("a"));
        assertEquals(Optional.of(new Moved<>("d")), ids.changeOf("c"));
    }

    @Test
    void theEldestChangesAreForgottenPastTheCapacity() {
        var few = new StaleIds<String>(2);
        few.deleted("a");
        few.deleted("b");
        few.deleted("c");

        assertTrue(few.changeOf("a").isEmpty());
        assertEquals(Optional.of(new Deleted<>()), few.changeOf("b"));
        assertEquals(Optional.of(new Deleted<>()), few.changeOf("c"));
    }
}
