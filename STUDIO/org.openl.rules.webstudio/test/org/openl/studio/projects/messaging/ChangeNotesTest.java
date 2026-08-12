package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ChangeNotesTest {

    @Test
    void a_change_made_by_a_request_names_its_client() {
        var notes = ChangeNotes.of(List.of("rules/A.xlsx"), "tab-1");

        assertEquals(Set.of("rules/A.xlsx"), notes.files());
        assertEquals(Set.of("tab-1"), notes.origins());
    }

    @Test
    void a_change_made_outside_a_request_names_no_origin() {
        assertEquals(Set.of(), ChangeNotes.of(List.of(), null).origins());
    }

    @Test
    void merging_keeps_every_origin_so_one_session_cannot_hide_another() {
        var merged = ChangeNotes.of(List.of("rules/A.xlsx"), "tab-1")
                .merge(ChangeNotes.of(List.of("rules/B.xlsx"), "tab-2"));

        assertEquals(Set.of("rules/A.xlsx", "rules/B.xlsx"), merged.files());
        // A ping standing for two clients is nobody's own echo: dropping it would lose tab-2's change.
        assertEquals(Set.of("tab-1", "tab-2"), merged.origins());
    }

    @Test
    void a_change_with_no_origin_leaves_the_ping_unattributed() {
        var merged = ChangeNotes.of(List.of(), "tab-1").merge(ChangeNotes.of(List.of("rules/A.xlsx"), null));

        // The files watcher saw a write it cannot attribute; the known origin still rides along.
        assertEquals(Set.of("rules/A.xlsx"), merged.files());
        assertEquals(Set.of("tab-1"), merged.origins());
    }
}
