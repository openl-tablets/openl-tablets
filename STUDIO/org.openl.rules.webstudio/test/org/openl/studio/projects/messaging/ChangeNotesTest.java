package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ChangeNotesTest {

    @Test
    void a_change_made_by_a_request_names_its_client() {
        var notes = ChangeNotes.of(List.of("rules/A.xlsx"), Set.of("tab-1"));

        assertEquals(Set.of("rules/A.xlsx"), notes.files());
        assertEquals(Set.of("tab-1"), notes.origins());
    }

    @Test
    void a_change_made_outside_a_request_names_no_origin() {
        assertEquals(Set.of(), ChangeNotes.of(List.of(), Set.of()).origins());
    }
}
