package org.openl.studio.audit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AuditChangesTest {

    @Test
    void notesOnlyTheFieldsThatChanged() {
        var changes = new AuditChanges()
                .compare("firstName", "John", "John")
                .compare("email", "jdoe@example.com", "john.doe@example.com")
                .compare("emailVerified", false, true);

        assertEquals(Map.of(
                "email", Map.of("from", "jdoe@example.com", "to", "john.doe@example.com"),
                "emailVerified", Map.of("from", false, "to", true)), changes.asMap());
    }

    @Test
    void countsABlankTextAsNoValue() {
        var changes = new AuditChanges()
                .compare("firstName", null, "")
                .compare("lastName", " ", null);

        assertTrue(changes.isEmpty());
        var cleared = new AuditChanges().compare("description", "Read the rules", "");
        var change = (Map<?, ?>) cleared.asMap().get("description");
        assertEquals("Read the rules", change.get("from"));
        assertTrue(change.containsKey("to"));
        assertNull(change.get("to"));
    }

    @Test
    void tellsTheNamesAddedAndRemoved() {
        var before = List.of("Analysts", "Testers");
        var after = List.of("Testers", "Deployers", "Analysts", "Billing");

        assertEquals(Set.of("Billing", "Deployers"), AuditChanges.added(before, after));
        assertEquals(List.of("Billing", "Deployers"), List.copyOf(AuditChanges.added(before, after)), "Sorted");
        assertEquals(Set.of(), AuditChanges.removed(before, after));
        assertEquals(Set.of("Billing", "Deployers"), AuditChanges.removed(after, before));
        assertEquals(Map.of("added", Set.of("Billing", "Deployers"), "removed", Set.of()),
                AuditChanges.addedAndRemoved(before, after));
        assertEquals(Map.of(), AuditChanges.addedAndRemoved(after, after), "Nothing changed");
    }
}
