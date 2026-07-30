package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class RulesXmlMigrationsMethodFilterTest {

    @Test
    void testSingleModuleWithIncludes() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ calculate\\(.+\\)"}, null));

        RulesXmlMigrations.methodFilter(pd);

        // Module-level filter should be cleared
        assertEmptyMethodFilter(pd.getModules().getFirst());
        // Project-level exposed-methods should contain converted pattern
        assertNotNull(pd.getExposedMethods());
        assertTrue(pd.getExposedMethods().getIncludes().contains("calculate"));
    }

    @Test
    void testSingleModuleWithExcludes() {
        var pd = createProject("TestProject",
                createModule("Module1", null, new String[]{".* getDiscount(.*)"}));

        RulesXmlMigrations.methodFilter(pd);

        assertEmptyMethodFilter(pd.getModules().getFirst());
        assertNotNull(pd.getExposedMethods());
        assertNull(pd.getExposedMethods().getIncludes());
        assertTrue(pd.getExposedMethods().getExcludes().contains("getDiscount*"));
    }

    @Test
    void testSingleModuleWithIncludesAndExcludes() {
        var pd = createProject("TestProject",
                createModule("Module1",
                        new String[]{".+ calculate\\(.+\\)", ".+ getRate\\(.*\\)"},
                        new String[]{".* internal(.*)"}));

        RulesXmlMigrations.methodFilter(pd);

        assertEmptyMethodFilter(pd.getModules().getFirst());
        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(2, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("calculate"));
        assertTrue(im.getIncludes().contains("getRate"));
        assertEquals(1, im.getExcludes().size());
        assertTrue(im.getExcludes().contains("internal*"));
    }

    @Test
    void testMultipleModulesAggregated() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ foo\\(.+\\)"}, null),
                createModule("Module2", new String[]{".+ bar\\(.*\\)"}, null));

        RulesXmlMigrations.methodFilter(pd);

        // Both modules should be cleared
        for (var module : pd.getModules()) {
            assertEmptyMethodFilter(module);
        }
        // Patterns from both modules should be merged
        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(2, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("foo"));
        assertTrue(im.getIncludes().contains("bar"));
    }

    @Test
    void testDuplicatePatternsDeduped() {
        // Both modules include the same regex, should result in a single glob entry
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ calculate\\(.+\\)"}, null),
                createModule("Module2", new String[]{".+ calculate\\(.+\\)"}, null));

        RulesXmlMigrations.methodFilter(pd);

        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(1, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("calculate"));
    }

    @Test
    void testMergeWithExistingExposedMethods() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ newMethod\\(.+\\)"}, null));
        // Set existing exposed-methods
        var existing = new ExposedMethods();
        existing.setIncludes(Set.of("existingMethod"));
        pd.setExposedMethods(existing);

        RulesXmlMigrations.methodFilter(pd);

        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(2, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("newMethod"));
        assertTrue(im.getIncludes().contains("existingMethod"));
    }

    @Test
    void testInvalidPatternsIgnored() {
        // "*" is not valid regex, should be ignored
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{"*", "not_matching_anything"}, null));

        RulesXmlMigrations.methodFilter(pd);

        assertEmptyMethodFilter(pd.getModules().getFirst());
        // No valid patterns means no exposed-methods
        assertNull(pd.getExposedMethods());
    }

    @Test
    void testMatchAllPattern() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".*"}, null));

        RulesXmlMigrations.methodFilter(pd);

        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(1, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("*"));
    }

    @Test
    void testNoMethodFilters() {
        var pd = createProject("TestProject",
                createModule("Module1", null, null));

        RulesXmlMigrations.methodFilter(pd);

        // No exposed-methods should be set
        assertNull(pd.getExposedMethods());
    }

    @Test
    void testWrappedWildcardPattern() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".*determinePolicyPremium.*"}, null));

        RulesXmlMigrations.methodFilter(pd);

        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertTrue(im.getIncludes().contains("*determinePolicyPremium*"));
    }

    @Test
    void testRealWorldPatterns() {
        // Patterns from the actual OpenL codebase
        var pd = createProject("TestProject",
                createModule("Algorithms",
                        new String[]{".+ RatingBasis1\\(.+\\)", ".+ RatingBasis\\(.+\\)"},
                        null),
                createModule("REST",
                        new String[]{
                                ".+ PlanDetailsPUT\\(.+\\)",
                                ".+ PlanDetailsGET\\(.+\\)",
                                ".+ PlanDetailsPOST\\(.+\\)"
                        },
                        null));

        RulesXmlMigrations.methodFilter(pd);

        var im = pd.getExposedMethods();
        assertNotNull(im);
        assertEquals(5, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("RatingBasis1"));
        assertTrue(im.getIncludes().contains("RatingBasis"));
        assertTrue(im.getIncludes().contains("PlanDetailsPUT"));
        assertTrue(im.getIncludes().contains("PlanDetailsGET"));
        assertTrue(im.getIncludes().contains("PlanDetailsPOST"));
    }

    private static ProjectDescriptor createProject(String name, Module... modules) {
        var pd = new ProjectDescriptor();
        pd.setName(name);
        pd.setModules(new ArrayList<>(List.of(modules)));
        return pd;
    }

    private static Module createModule(String name, String[] includes, String[] excludes) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(name + ".xlsx");
        if (includes != null || excludes != null) {
            var filter = new MethodFilter();
            if (includes != null) {
                filter.addIncludePattern(includes);
            }
            if (excludes != null) {
                filter.addExcludePattern(excludes);
            }
            module.setMethodFilter(filter);
        }
        return module;
    }

    private static void assertEmptyMethodFilter(Module module) {
        var mf = module.getMethodFilter();
        if (mf != null) {
            assertTrue(mf.getIncludes() == null || mf.getIncludes().isEmpty(),
                    "Expected empty includes but was: " + mf.getIncludes());
            assertTrue(mf.getExcludes() == null || mf.getExcludes().isEmpty(),
                    "Expected empty excludes but was: " + mf.getExcludes());
        }
    }
}
