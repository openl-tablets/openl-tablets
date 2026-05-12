package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.maven.migration.Migrator;

class MigrateMojoSelectionTest {

    private static final List<Migrator> ALL = MigrateMojo.allMigrators(() -> false);

    @Test
    void nullSelectorReturnsEveryMigrator() {
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, null)));
    }

    @Test
    void emptySelectorReturnsEveryMigrator() {
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, List.of())));
    }

    @Test
    void blankAndNullEntriesAreIgnored() {
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, Arrays.asList("   ", "\t", null))));
    }

    @Test
    void allSelectorReturnsEveryMigrator() {
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, List.of("all"))));
    }

    @Test
    void allSelectorWinsOverPeers() {
        // 'all' wins regardless of position or co-selectors. Mixed lists collapse to the full set.
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, List.of("groovy", "all"))));
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, List.of("all", "config.deploy"))));
        // Also works as one comma-separated CLI value.
        assertEquals(allIds(), ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty-tag,all"))));
    }

    @Test
    void exactIdMatchesOnlyThatMigrator() {
        assertEquals(ids("config.empty-tag"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty-tag"))));
        assertEquals(ids("config.project.classpath"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.classpath"))));
        assertEquals(ids("config.project.lib"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.lib"))));
        assertEquals(ids("config.project.cw-processor"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.cw-processor"))));
        assertEquals(ids("config.project.method-filter"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.method-filter"))));
        assertEquals(ids("config.project.default-modules"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.default-modules"))));
        assertEquals(ids("config.deploy.template-class"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.deploy.template-class"))));
        assertEquals(ids("groovy.jakarta"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("groovy.jakarta"))));
    }

    @Test
    void categoryPrefixMatchesEveryMigratorInCategory() {
        // The merged empty-tag migrator lives directly under `config`, so `config.project` and `config.deploy`
        // each match only the content-specific migrators of their file.
        assertEquals(ids("config.project.lib",
                        "config.project.classpath",
                        "config.project.cw-processor",
                        "config.project.method-filter",
                        "config.project.default-modules"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project"))));
        assertEquals(ids("config.deploy.runtime-context",
                        "config.deploy.template-class"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.deploy"))));
        // `config` covers everything below it, including the shared empty-tag phase.
        assertEquals(allConfigIds(),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config"))));
        assertEquals(ids("groovy.jakarta"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("groovy"))));
    }

    @Test
    void multipleSelectorsUnion() {
        assertEquals(ids("config.empty-tag", "groovy.jakarta"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty-tag", "groovy.jakarta"))));
        assertEquals(ids("config.empty-tag",
                        "config.project.lib",
                        "config.project.classpath",
                        "config.project.cw-processor",
                        "config.project.method-filter",
                        "config.project.default-modules",
                        "groovy.jakarta"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty-tag", "config.project", "groovy"))));
    }

    @Test
    void commaSeparatedEntryIsSplitForCliCompatibility() {
        // Maven delivers `-Dopenl.migrate.migrators=a,b` as a single-element list. Same result expected.
        assertEquals(ids("config.empty-tag", "groovy.jakarta"),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty-tag,groovy.jakarta"))));
    }

    @Test
    void unknownSelectorMatchesNothing() {
        assertEquals(List.of(), ids(MigrateMojo.selectMigrators(ALL, List.of("nope"))));
    }

    @Test
    void prefixMustBeFullSegmentNotSubstring() {
        // "conf" must not match "config.*".
        assertEquals(List.of(), ids(MigrateMojo.selectMigrators(ALL, List.of("conf"))));
        // "config.proj" must not match "config.project.*".
        assertEquals(List.of(), ids(MigrateMojo.selectMigrators(ALL, List.of("config.proj"))));
        // "config.deploy.drop" must not match "config.deploy.drop-rmi".
        assertEquals(List.of(), ids(MigrateMojo.selectMigrators(ALL, List.of("config.deploy.drop"))));
        // "config.project.wildcard" must not match "config.project.wildcard-module-name".
        assertEquals(List.of(),
                ids(MigrateMojo.selectMigrators(ALL, List.of("config.project.wildcard"))));
        // "config.empty" must not match "config.empty-tag" — the selector boundary is the dot,
        // not a substring.
        assertEquals(List.of(), ids(MigrateMojo.selectMigrators(ALL, List.of("config.empty"))));
    }

    @Test
    void commitTemplateExpandsAllPlaceholders() {
        var template = "@{prefix} @{message} for OpenL @{version}\n\n"
                + "Co-authored-by: openl-maven-plugin:@{version} <openltablets@eisgroup.com>";

        assertEquals(
                "migrate:  method-filter to exposed-methods for OpenL 6.1.0-SNAPSHOT\n\n"
                        + "Co-authored-by: openl-maven-plugin:6.1.0-SNAPSHOT <openltablets@eisgroup.com>",
                MigrateMojo.renderCommitTemplate(template, "migrate: ", "method-filter to exposed-methods", "6.1.0-SNAPSHOT"));
    }

    @Test
    void commitTemplateExpandsPlaceholdersForEveryMigrator() {
        var template = "@{prefix}@{message} (v@{version})";
        for (var migrator : ALL) {
            assertEquals(
                    "migrate: " + migrator.getCommitMessage() + " (v1.2.3)",
                    MigrateMojo.renderCommitTemplate(template, "migrate: ", migrator.getCommitMessage(), "1.2.3"));
        }
    }

    @Test
    void commitTemplateAcceptsNullPlaceholderValues() {
        assertEquals("(/)",
                MigrateMojo.renderCommitTemplate("(@{prefix}@{message}/@{version})", null, null, null));
    }

    @Test
    void commitTemplateLeavesUnknownPlaceholdersIntact() {
        assertEquals("@{ticket} migrate: drop RMI configs",
                MigrateMojo.renderCommitTemplate("@{ticket} @{prefix}@{message}", "migrate: ", "drop RMI configs", "1"));
    }

    @Test
    void allMigratorsAlphabetical_listsEveryKnownId() {
        var sortedIds = MigrateMojo.allMigratorsAlphabetical().stream().map(Migrator::getId).toList();
        for (var expectedId : allIds()) {
            assertTrue(sortedIds.contains(expectedId), () -> "missing migrator id in listing: " + expectedId);
        }
    }

    @Test
    void allMigratorsAlphabetical_isStrictlySortedById() {
        // The migrate-list goal's whole point is a stable, alphabetically sorted listing — guard against
        // future drift in case someone replaces the comparator or returns the execution-order list by accident.
        var sortedIds = MigrateMojo.allMigratorsAlphabetical().stream().map(Migrator::getId).toList();
        var expected = sortedIds.stream().sorted().toList();
        assertEquals(expected, sortedIds);
    }

    private static List<String> allIds() {
        return List.of("config.empty-tag",
                "config.project.lib",
                "config.project.classpath",
                "config.project.cw-processor",
                "config.project.method-filter",
                "config.project.default-modules",
                "config.deploy.runtime-context",
                "config.deploy.template-class",
                "groovy.jakarta");
    }

    private static List<String> allConfigIds() {
        return List.of("config.empty-tag",
                "config.project.lib",
                "config.project.classpath",
                "config.project.cw-processor",
                "config.project.method-filter",
                "config.project.default-modules",
                "config.deploy.runtime-context",
                "config.deploy.template-class");
    }

    private static List<String> ids(String... ids) {
        return List.of(ids);
    }

    private static List<String> ids(List<Migrator> migrators) {
        return migrators.stream().map(Migrator::getId).toList();
    }
}
