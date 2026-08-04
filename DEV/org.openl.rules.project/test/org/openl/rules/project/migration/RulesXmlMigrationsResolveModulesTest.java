package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Resolving a descriptor's modules against a project's workbook files — the check a migration uses to refuse
 * turning an undeclared workbook into a module.
 */
class RulesXmlMigrationsResolveModulesTest {

    private static final List<String> FILES = List.of(
            "rules/Main.xlsx",
            "rules/Extra.xlsx",
            "rules/sub/Deep.xlsx",
            "tests/Cases.xlsx",
            "other/Different.xlsx");

    @Test
    void concreteModuleResolvesToItsOwnWorkbookOnly() {
        assertResolves("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, Set.of("rules/Main.xlsx"));
    }

    @Test
    void namedModuleStillResolvesToItsWorkbook() {
        assertResolves("""
                <project>
                    <modules>
                        <module>
                            <name>CustomLabel</name>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, Set.of("rules/Main.xlsx"));
    }

    @Test
    void absentModulesResolveToTheEngineDefaults() {
        // rules/** and tests/** match; other/ is not a default folder, so Different.xlsx is not a module.
        assertResolves("""
                <project>
                    <name>explicit-project</name>
                </project>
                """, Set.of("rules/Main.xlsx", "rules/Extra.xlsx", "rules/sub/Deep.xlsx", "tests/Cases.xlsx"));
    }

    @Test
    void defaultsResolveXlsAndXlsmWorkbooksToo() {
        // The widening guard sees .xls/.xlsm under rules/ and tests/ now that the engine defaults cover them;
        // other/ is not a default folder, so its .xls is not a module.
        var files = List.of("rules/Legacy.xls", "rules/Main.xlsx", "tests/Old.xlsm", "other/Skip.xls");
        assertEquals(Set.of("rules/Legacy.xls", "rules/Main.xlsx", "tests/Old.xlsm"),
                resolveModuleWorkbooks("<project/>", files));
    }

    @Test
    void recursiveWildcardMatchesNestedWorkbooks() {
        assertResolves("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/**/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, Set.of("rules/Main.xlsx", "rules/Extra.xlsx", "rules/sub/Deep.xlsx"));
    }

    @Test
    void singleStarWildcardMatchesOneSegmentOnly() {
        // rules/*.xlsx stops at the folder boundary, so the nested rules/sub/Deep.xlsx is not matched.
        assertResolves("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, Set.of("rules/Main.xlsx", "rules/Extra.xlsx"));
    }

    @Test
    void collapsingConcreteRulesModuleToDefaultsWidensTheSet() {
        // The reported defect: rules/Main.xlsx declared alone resolves to one module, but the migrated
        // descriptor (no <modules> → defaults) pulls in every sibling. The added set is what a migrate refuses.
        var before = resolve("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);
        var after = resolve("""
                <project>
                </project>
                """);
        var added = RulesXmlMigrations.addedWorkbooks(before, after);
        assertEquals(List.of("rules/Extra.xlsx", "rules/sub/Deep.xlsx", "tests/Cases.xlsx"), added);
    }

    @Test
    void collapsingNonDefaultFolderToWildcardWidensTheSet() {
        // other/Different.xlsx declared alone; rewriting it to other/**/*.xlsx absorbs its undeclared sibling.
        var files = List.of("other/Different.xlsx", "other/Sibling.xlsx");
        var before = resolveModuleWorkbooks("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="other/Different.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, files);
        var after = resolveModuleWorkbooks("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="other/**/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, files);
        var added = RulesXmlMigrations.addedWorkbooks(before, after);
        assertEquals(List.of("other/Sibling.xlsx"), added);
    }

    private static void assertResolves(String xml, Set<String> expected) {
        assertEquals(expected, resolve(xml));
    }

    private static Set<String> resolve(String xml) {
        return resolveModuleWorkbooks(xml, FILES);
    }

    private static Set<String> resolveModuleWorkbooks(String xml, List<String> files) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
    }
}
