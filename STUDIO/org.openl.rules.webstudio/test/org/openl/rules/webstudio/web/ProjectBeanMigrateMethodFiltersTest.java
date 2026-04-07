package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

class ProjectBeanMigrateMethodFiltersTest {

    @Test
    void testSingleModuleWithIncludes() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ calculate\\(.+\\)"}, null));

        var result = ProjectBean._migrateMethodFilters(pd);

        // Module-level filter should be cleared
        assertEmptyMethodFilter(result.getModules().getFirst());
        // Project-level interface-methods should contain converted pattern
        assertNotNull(result.getInterfaceMethods());
        assertTrue(result.getInterfaceMethods().getIncludes().contains("calculate"));
    }

    @Test
    void testSingleModuleWithExcludes() {
        var pd = createProject("TestProject",
                createModule("Module1", null, new String[]{".* getDiscount(.*)"}));

        var result = ProjectBean._migrateMethodFilters(pd);

        assertEmptyMethodFilter(result.getModules().getFirst());
        assertNotNull(result.getInterfaceMethods());
        assertNull(result.getInterfaceMethods().getIncludes());
        assertTrue(result.getInterfaceMethods().getExcludes().contains("getDiscount*"));
    }

    @Test
    void testSingleModuleWithIncludesAndExcludes() {
        var pd = createProject("TestProject",
                createModule("Module1",
                        new String[]{".+ calculate\\(.+\\)", ".+ getRate\\(.*\\)"},
                        new String[]{".* internal(.*)"}));

        var result = ProjectBean._migrateMethodFilters(pd);

        assertEmptyMethodFilter(result.getModules().getFirst());
        var im = result.getInterfaceMethods();
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

        var result = ProjectBean._migrateMethodFilters(pd);

        // Both modules should be cleared
        for (var module : result.getModules()) {
            assertEmptyMethodFilter(module);
        }
        // Patterns from both modules should be merged
        var im = result.getInterfaceMethods();
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

        var result = ProjectBean._migrateMethodFilters(pd);

        var im = result.getInterfaceMethods();
        assertNotNull(im);
        assertEquals(1, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("calculate"));
    }

    @Test
    void testMergeWithExistingInterfaceMethods() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ newMethod\\(.+\\)"}, null));
        // Set existing interface-methods
        var existing = new MethodFilter();
        existing.addIncludePattern("existingMethod");
        pd.setInterfaceMethods(existing);

        var result = ProjectBean._migrateMethodFilters(pd);

        var im = result.getInterfaceMethods();
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

        var result = ProjectBean._migrateMethodFilters(pd);

        assertEmptyMethodFilter(result.getModules().getFirst());
        // No valid patterns means no interface-methods
        assertNull(result.getInterfaceMethods());
    }

    @Test
    void testMatchAllPattern() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".*"}, null));

        var result = ProjectBean._migrateMethodFilters(pd);

        var im = result.getInterfaceMethods();
        assertNotNull(im);
        assertEquals(1, im.getIncludes().size());
        assertTrue(im.getIncludes().contains("*"));
    }

    @Test
    void testNoMethodFilters() {
        var pd = createProject("TestProject",
                createModule("Module1", null, null));

        var result = ProjectBean._migrateMethodFilters(pd);

        // No interface-methods should be set
        assertNull(result.getInterfaceMethods());
    }

    @Test
    void testWrappedWildcardPattern() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".*determinePolicyPremium.*"}, null));

        var result = ProjectBean._migrateMethodFilters(pd);

        var im = result.getInterfaceMethods();
        assertNotNull(im);
        assertTrue(im.getIncludes().contains("*determinePolicyPremium*"));
    }

    @Test
    void testOriginalDescriptorNotModified() {
        var pd = createProject("TestProject",
                createModule("Module1", new String[]{".+ foo\\(.+\\)"}, null));
        var originalIncludes = Set.copyOf(pd.getModules().getFirst().getMethodFilter().getIncludes());

        var result = ProjectBean._migrateMethodFilters(pd);

        assertNotSame(pd, result);
        // Original descriptor should still have its method-filter
        assertEquals(originalIncludes, pd.getModules().getFirst().getMethodFilter().getIncludes());
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

        var result = ProjectBean._migrateMethodFilters(pd);

        var im = result.getInterfaceMethods();
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
        pd.setModules(List.of(modules));
        return pd;
    }

    private static Module createModule(String name, String[] includes, String[] excludes) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(new PathEntry(name + ".xlsx"));
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
