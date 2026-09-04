package org.openl.rules.project.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class RulesXmlMigrationsTest {

    @Test
    void runsTheContentMigrationsAndDropsTheDefaultClasspath() {
        var descriptor = new ProjectDescriptor();
        descriptor.setClasspath(new ArrayList<>(List.of("groovy/", "lib/*.jar")));

        RulesXmlMigrations.apply(descriptor);

        assertNull(descriptor.getClasspath());
    }

    @Test
    void liftsModuleMethodFilterToExposedMethodsThenCollapsesTheModules() {
        // A project whose single rules/*.xlsx module carries a method-filter, with the default classpath —
        // the shape reported as migrating only the classpath.
        var module = new Module();
        module.setRulesRootPath("rules/*.xlsx");
        var filter = new MethodFilter();
        filter.addIncludePattern(
                ".+ AgeBandsList\\(\\)",
                ".+ InitializeExperienceRating\\(.+\\)",
                ".+ PolicyOverrideCalculation\\(.+\\)",
                ".+ ApplyChanges\\(.+\\)",
                ".+ ExtractManualRating\\(.+\\)",
                ".+ ExtractPolicy\\(.+\\)",
                ".+ DeterminePolicyRatesAndPremiums\\(.+\\)");
        module.setMethodFilter(filter);
        var descriptor = new ProjectDescriptor();
        descriptor.setName("RSL GTL Rating_Deploy_VG");
        descriptor.setModules(new ArrayList<>(List.of(module)));
        descriptor.setClasspath(new ArrayList<>(List.of("groovy", "lib/*.jar")));

        RulesXmlMigrations.apply(descriptor);

        // The method-filter is lifted to a project-level <exposed-methods>, freeing the module to collapse
        // into the default wildcard, so the whole <modules> block drops. The classpath is the default one.
        assertNull(descriptor.getClasspath());
        assertNull(descriptor.getModules());
        assertEquals("RSL GTL Rating_Deploy_VG", descriptor.getName());
        var exposed = descriptor.getExposedMethods();
        assertNull(exposed.getExcludes());
        assertEquals(
                Set.of("AgeBandsList", "InitializeExperienceRating", "PolicyOverrideCalculation", "ApplyChanges",
                        "ExtractManualRating", "ExtractPolicy", "DeterminePolicyRatesAndPremiums"),
                exposed.getIncludes());
    }

    @Test
    void keepsAnUnconvertibleFilterInsteadOfDroppingIt() {
        // "get*" is not a method-signature regexp, so the no-compile path cannot lift it. The filter is kept
        // in place — not dropped — so the module stays explicit and the exposed API is not widened.
        var module = new Module();
        module.setName("Pricing");
        module.setRulesRootPath("rules/Pricing.xlsx");
        var filter = new MethodFilter();
        filter.addIncludePattern("get*");
        module.setMethodFilter(filter);
        var descriptor = new ProjectDescriptor();
        descriptor.setModules(new ArrayList<>(List.of(module)));

        RulesXmlMigrations.apply(descriptor);

        assertNull(descriptor.getExposedMethods());
        assertEquals(1, descriptor.getModules().size());
        var kept = descriptor.getModules().getFirst();
        assertEquals("rules/Pricing.xlsx", kept.getRulesRootPath());
        assertNotNull(kept.getMethodFilter());
        assertTrue(kept.getMethodFilter().getIncludes().contains("get*"));
    }

    @Test
    void doesNotFailOnAModuleWithoutARulesRoot() {
        // A <module> that omits <rules-root> parses with a null path; the migration must not dereference it.
        var descriptor = new ProjectDescriptor();
        descriptor.setModules(new ArrayList<>(List.of(new Module())));

        RulesXmlMigrations.apply(descriptor);

        // The rules-root-less module cannot be collapsed, so it is left as declared.
        assertEquals(1, descriptor.getModules().size());
    }
}
