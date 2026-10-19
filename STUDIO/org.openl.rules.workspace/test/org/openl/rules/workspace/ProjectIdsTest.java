package org.openl.rules.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProjectIdsTest {

    @Test
    void anIdIsTwelveUrlSafeCharactersOfTheRepositoryAndTheFolder() {
        assertEquals("FeZdnMnl3XRa", ProjectIds.of("design", "Example 1 - Bank Rating").value());
        assertEquals("LhFSURkIYmQQ", ProjectIds.of("design", "rating/Auto").value());
        assertEquals("c6KaddxZ4Oae", ProjectIds.of("jdbc", "DESIGN/rules/Bank Rating").value());
    }

    @Test
    void theIdHasNeitherPaddingNorSlashes() {
        for (var folder : List.of("Тарифный план", "a", "rules/Пример 1", "x/y/z", "Договор")) {
            var id = ProjectIds.of("design", folder).value();
            assertEquals(12, id.length(), folder);
            assertTrue(id.matches("[A-Za-z0-9_-]{12}"), id);
        }
    }

    @Test
    void onlyTwelveUrlSafeCharactersHaveTheFormOfAnId() {
        assertTrue(ProjectIds.isId("FeZdnMnl3XRa"));
        assertTrue(ProjectIds.isId("a-b_c-d_e-f_"));
        assertFalse(ProjectIds.isId("ZGVzaWduOlByb2plY3Q="));
        assertFalse(ProjectIds.isId("FeZdnMnl3XR"));
        assertFalse(ProjectIds.isId("FeZdnMnl3X/a"));
        assertFalse(ProjectIds.isId("Bank Rating"));
    }

    @Test
    void theRepositoryTellsProjectsOfOneFolderApart() {
        assertNotEquals(ProjectIds.of("design", "Bank"), ProjectIds.of("design1", "Bank"));
    }

    @Test
    void theFolderKeepsItsCase() {
        assertNotEquals(ProjectIds.of("design", "Bank"), ProjectIds.of("design", "bank"));
    }

    @Test
    void theFolderIsReadWithForwardSlashesAndNoSlashAtEitherEnd() {
        var id = ProjectIds.of("design", "rating/Auto");
        assertEquals(id, ProjectIds.of("design", "/rating/Auto/"));
        assertEquals(id, ProjectIds.of("design", "rating\\Auto"));
        assertEquals(id, ProjectIds.of("design", "//rating/Auto//"));
    }

    @Test
    void aDecomposedNameHasTheIdOfTheComposedOne() {
        assertEquals(ProjectIds.of("design", "Café"), ProjectIds.of("design", "Café"));
    }

    @Test
    void anIndexFindsEveryProjectByItsId() {
        var bank = new ProjectKey("design", "Bank");
        var auto = new ProjectKey("design", "rating/Auto");

        var index = ProjectIds.index(List.of(bank, auto), Function.identity());

        assertEquals(Map.of(ProjectIds.of("design", "Bank"), bank,
                ProjectIds.of("design", "rating/Auto"), auto), index);
    }

    @Test
    void identitiesSharingADigestAreNumberedInTheirOrder() {
        // A real 72-bit collision cannot be produced, so every identity of this repository hashes to one digest.
        var collision = "collision-" + System.nanoTime();
        Function<String, ProjectId> digest = identity -> identity.contains(collision) && !identity.contains("~")
                ? new ProjectId("shared")
                : ProjectIds.of("digest", identity);
        var first = new ProjectKey(collision, "a");
        var second = new ProjectKey(collision, "b");
        var third = new ProjectKey(collision, "c");

        var index = ProjectIds.index(List.of(third, first, second), Function.identity(), digest);

        assertEquals(3, index.size());
        assertEquals(first, index.get(new ProjectId("shared")));
        var secondId = ProjectIds.of(collision, "b");
        var thirdId = ProjectIds.of(collision, "c");
        assertEquals(second, index.get(secondId));
        assertEquals(third, index.get(thirdId));
        assertEquals(digest.apply(ProjectIds.identity(collision, "b") + "\u001F~2"), secondId);
        assertEquals(digest.apply(ProjectIds.identity(collision, "c") + "\u001F~2"), thirdId);
    }

    @Test
    void anIdIsWrittenToJsonAsItsText() throws Exception {
        var mapper = new ObjectMapper();
        var id = new ProjectId("FeZdnMnl3XRa");

        assertEquals("\"FeZdnMnl3XRa\"", mapper.writeValueAsString(id));
        assertEquals(id, mapper.readValue("\"FeZdnMnl3XRa\"", ProjectId.class));
        assertEquals("FeZdnMnl3XRa", id.toString());
        assertEquals(id, ProjectId.of("FeZdnMnl3XRa"));
    }

    @Test
    void anIdNeedsText() {
        assertThrows(NullPointerException.class, () -> new ProjectId(null));
    }
}
