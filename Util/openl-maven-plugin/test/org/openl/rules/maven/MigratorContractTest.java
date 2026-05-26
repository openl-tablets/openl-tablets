package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Cross-cutting contract checks every {@link org.openl.rules.maven.migration.Migrator} implementation has
 * to honour. Spawned via {@link TestFactory} so a failure points at the specific migrator id (and the
 * failing rule) instead of one anonymous "MigratorContractTest" entry.
 */
class MigratorContractTest {

    /**
     * {@link org.openl.rules.maven.migration.Migrator#getDescription()} must be 10 to 100 words. Short
     * enough that {@code openl:migrate-list} can render the whole list at once; long enough to make a
     * user understand what the migrator does and what assumptions it carries. Most fit in well under 50
     * words; the extra headroom is for the few migrators whose behaviour needs careful explanation.
     */
    @TestFactory
    List<DynamicTest> descriptionLengthIsWithinTenToHundredWords() {
        return MigrateMojo.allMigratorsAlphabetical().stream()
                .map(m -> DynamicTest.dynamicTest(m.getId(), () -> {
                    var description = m.getDescription();
                    assertNotNull(description, "getDescription() returned null");
                    assertFalse(description.isBlank(), "getDescription() returned blank");
                    var words = description.trim().split("\\s+").length;
                    assertTrue(words >= 10 && words <= 100,
                            () -> "description must be 10-100 words, got " + words + ":\n" + description);
                }))
                .toList();
    }

    /**
     * Migrators must keep a non-blank id and commit message too — the listing depends on both.
     */
    @TestFactory
    List<DynamicTest> idAndCommitMessageAreNonBlank() {
        return MigrateMojo.allMigratorsAlphabetical().stream()
                .map(m -> DynamicTest.dynamicTest(m.getClass().getSimpleName(), () -> {
                    assertNotNull(m.getId(), "getId() returned null");
                    assertFalse(m.getId().isBlank(), "getId() returned blank");
                    assertNotNull(m.getCommitMessage(), "getCommitMessage() returned null");
                    assertFalse(m.getCommitMessage().isBlank(), "getCommitMessage() returned blank");
                }))
                .toList();
    }
}
