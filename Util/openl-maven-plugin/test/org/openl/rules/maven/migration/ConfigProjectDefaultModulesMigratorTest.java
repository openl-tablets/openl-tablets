package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

class ConfigProjectDefaultModulesMigratorTest {

    // --- Rule 1: drop redundant <name> on a module --------------------------------------------------

    @Test
    void clearsNameWhenPathHasStarWildcard() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>main</name>
                                    <rules-root path="rules/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void clearsNameWhenPathHasQuestionMarkWildcard() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>main</name>
                                    <rules-root path="rules/?.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void clearsOnlyWildcardModulesInMixedList() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>legacy-wildcard</name>
                                    <rules-root path="rules/*.xlsx"/>
                                </module>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void clearsModuleNameWhenItEqualsRulesRootBasename() {
        // Rule 1 clears the redundant name → rule 2 collapses to rules/**/*.xlsx → rule 3 drops the
        // default wildcard. Cascading is by design: an explicit module that only restates a default is
        // worth nothing.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>Hello</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void keepsModuleNameWhenDifferentFromBasename() {
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    // --- Rule 2: union name-less modules into <subfolder>/**/*.xlsx wildcards ---------------------

    @Test
    void collapsesNamelessModulesInRulesSubfolderIntoDefaultWildcard() {
        // After collapse the only module is rules/**/*.xlsx — a default — so rule 3 drops <modules>.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="rules/B.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="rules/sub/C.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void collapsesNamelessModulesInNonDefaultSubfolderAndKeepsTheWildcard() {
        // data/ is not a default subfolder, so rule 3 leaves the wildcard in place.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="data/A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="data/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void collapsesAcrossMultipleSubfoldersIntoSeparateWildcards() {
        // Different subfolders are each collapsed independently. Both happen to be defaults here, so
        // rule 3 then drops the whole block.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="tests/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void collapsesAcrossMixedSubfoldersAndKeepsNonDefaultWildcards() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void collapsesAnonymousModuleAndKeepsNamedNeighbour() {
        // Only the name-less module is unioned into a wildcard; the named neighbour passes through and
        // keeps the <modules> block from being dropped by rule 3.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <name>explicit</name>
                                    <rules-root path="rules/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>explicit</name>
                                    <rules-root path="rules/B.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void collapsesAnonymousNonWildcardIntoExistingWildcardForSameFolder() {
        // The anonymous non-wildcard's folder is already represented by the sibling wildcard, so the
        // collapse only normalises the wildcard in place rather than appending a duplicate. Both then
        // get dropped by rule 3 because the result is a single default wildcard.
        assertMigration("""
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="rules/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                        """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void collapsesAnonymousInOtherFolderWhenWildcardExistsElsewhere() {
        // The wildcard only protects its own folder; anonymous modules in other folders still collapse.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/*.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/A.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void doesNotCollapseRootLevelModules() {
        // No top-level subfolder means there's nothing to wildcard over.
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="A.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    // --- Rule 3: drop <modules> when every entry is a default wildcard ----------------------------

    @Test
    void dropsModulesWhenOnlyRulesDefaultWildcard() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void dropsModulesWhenOnlyTestsDefaultWildcard() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="tests/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void dropsModulesWhenBothRulesAndTestsDefaultWildcards() {
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="tests/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    @Test
    void doesNotDropModulesWhenDefaultWildcardCarriesMethodFilter() {
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                    <method-filter>
                                        <includes>
                                            <value>foo*</value>
                                        </includes>
                                    </method-filter>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void doesNotDropModulesWhenDefaultWildcardCarriesWebstudioConfig() {
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                    <webstudioConfiguration>
                                        <compileThisModuleOnly>true</compileThisModuleOnly>
                                    </webstudioConfiguration>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void doesNotDropModulesWhenAnyNonDefaultWildcardPresent() {
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                                <module>
                                    <rules-root path="data/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void dropsModulesWhenSingleDefaultWildcardHasExplicitName() {
        // Rule 1 clears the name first, then rule 3 drops the module entirely.
        assertMigration(
                """
                        <project>
                            <name>explicit-project</name>
                            <modules>
                                <module>
                                    <name>named</name>
                                    <rules-root path="rules/**/*.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    // --- Rule 4: drop project <name> when it equals folder name -----------------------------------

    @Test
    void dropsProjectNameWhenEqualsFolderName() {
        assertMigrationInFolder("my-project",
                """
                        <project>
                            <name>my-project</name>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                """
                        <project>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    @Test
    void keepsProjectNameWhenDifferentFromFolder() {
        assertUnchangedInFolder("other-folder",
                """
                        <project>
                            <name>explicit-name</name>
                            <modules>
                                <module>
                                    <name>CustomLabel</name>
                                    <rules-root path="rules/Hello.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """);
    }

    // --- Combined / boundary cases ----------------------------------------------------------------

    @Test
    void rewritesRulesXmlWhenAllDefaultsCanBeDropped() {
        assertMigrationInFolder("my-project",
                """
                        <project>
                            <name>my-project</name>
                            <modules>
                                <module>
                                    <name>A</name>
                                    <rules-root path="rules/A.xlsx"/>
                                </module>
                                <module>
                                    <name>B</name>
                                    <rules-root path="rules/B.xlsx"/>
                                </module>
                            </modules>
                        </project>
                        """,
                "<project/>\n");
    }

    @Test
    void leavesAlreadyMinimalRulesXmlUnchanged() {
        assertUnchanged(
                """
                        <project>
                            <name>explicit-project</name>
                        </project>
                        """);
    }

    /**
     * Parses {@code before} into a {@link ProjectDescriptor}, runs the migrator's transform, marshals
     * the result back to XML, and asserts it equals {@code after}.
     */
    private static void assertMigration(String before, String after) {
        assertMigrationInFolder(null, before, after);
    }

    /**
     * Like {@link #assertMigration} but sets {@code projectFolder} on the descriptor so the rule
     * "drop project {@code <name>} that equals folder name" has a folder to compare against.
     */
    private static void assertMigrationInFolder(String folderName, String before, String after) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(before.getBytes(StandardCharsets.UTF_8)));
        if (folderName != null) {
            descriptor.setProjectFolder(Path.of(folderName));
        }
        ConfigProjectDefaultModulesMigrator.transform(descriptor);
        var actual = new String(descriptor.toBytes(), StandardCharsets.UTF_8);
        assertEquals(after, actual);
    }

    private static void assertUnchanged(String content) {
        assertMigration(content, content);
    }

    private static void assertUnchangedInFolder(String folderName, String content) {
        assertMigrationInFolder(folderName, content, content);
    }
}
