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
    void anIdIsThirteenBase32CharactersOfTheRepositoryAndTheFolder() {
        assertEquals("2QK5V769WQEQ8", ProjectIds.of("design", "Example 1 - Bank Rating").value());
        assertEquals("5R8N4M8S11H68", ProjectIds.of("design", "rating/Auto").value());
        assertEquals("EEH9MXEWB7GED", ProjectIds.of("jdbc", "DESIGN/rules/Bank Rating").value());
    }

    @Test
    void theIdHasNeitherPaddingNorSlashes() {
        for (var folder : List.of("Тарифный план", "a", "rules/Пример 1", "x/y/z", "Договор")) {
            var id = ProjectIds.of("design", folder).value();
            assertEquals(13, id.length(), folder);
            assertTrue(id.matches("[0-9A-HJKMNP-TV-Z]{13}"), id);
        }
    }

    @Test
    void onlyThirteenUpperCaseBase32CharactersHaveTheFormOfAnId() {
        assertTrue(ProjectIds.isId("2QK5V769WQEQ8"));
        assertTrue(ProjectIds.isId("0123456789ABC"));
        assertFalse(ProjectIds.isId("2qk5v769wqeq8"));
        assertFalse(ProjectIds.isId("2QK5V769WQEQI"));
        assertFalse(ProjectIds.isId("2QK5V769WQEQ"));
        assertFalse(ProjectIds.isId("FeZdnMnl3XRa"));
        assertFalse(ProjectIds.isId("ZGVzaWduOlByb2plY3Q="));
        assertFalse(ProjectIds.isId("2QK5V769WQE/8"));
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
        var collision = "collision-" + System.nanoTime();
        var digest = collidingDigest(collision);
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
    void aFolderListedTwiceKeepsItsId() {
        // A folder of its own: a number the index gives would last for the rest of the JVM.
        var folder = "twice-" + System.nanoTime();
        Function<String, ProjectKey> inTheFolder = entry -> new ProjectKey("design", folder);
        var id = ProjectIds.of("design", folder);

        var index = ProjectIds.index(List.of("copy", "copy listed again"), inTheFolder);

        assertEquals(Map.of(id, "copy"), index);
        assertEquals(id, ProjectIds.of("design", folder));
        assertEquals(Map.of(id, "copy"), ProjectIds.index(List.of("copy"), inTheFolder));
    }

    @Test
    void foldersSharingADigestAreNumberedOnceWhenListedTwice() {
        var collision = "collision-twice-" + System.nanoTime();
        var digest = collidingDigest(collision);
        var first = new ProjectKey(collision, "a");
        var second = new ProjectKey(collision, "b");

        var index = ProjectIds.index(List.of(second, first, first, second), Function.identity(), digest);

        assertEquals(Map.of(new ProjectId("shared"), first,
                digest.apply(ProjectIds.identity(collision, "b") + "\u001F~2"), second), index);
    }

    @Test
    void anIdIsWrittenToJsonAsItsText() throws Exception {
        var mapper = new ObjectMapper();
        var id = new ProjectId("2QK5V769WQEQ8");

        assertEquals("\"2QK5V769WQEQ8\"", mapper.writeValueAsString(id));
        assertEquals(id, mapper.readValue("\"2QK5V769WQEQ8\"", ProjectId.class));
        assertEquals("2QK5V769WQEQ8", id.toString());
        assertEquals(id, ProjectId.of("2QK5V769WQEQ8"));
    }

    @Test
    void anIdNeedsText() {
        assertThrows(NullPointerException.class, () -> new ProjectId(null));
    }

    /**
     * A real 72-bit collision cannot be produced, so every identity of the given repository hashes to one digest,
     * and a numbered identity to a digest of its own.
     */
    private static Function<String, ProjectId> collidingDigest(String repositoryId) {
        return identity -> identity.contains(repositoryId) && !identity.contains("~")
                ? new ProjectId("shared")
                : ProjectIds.of("digest", identity);
    }
}
