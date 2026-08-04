package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

class ProjectBeanDeclaredModulesTest {

    @Test
    void chooseModulesSourceUsesDeclaredWhenItHasModules() {
        var declared = createProject("declared", createModule("Rules"));
        var resolved = createProject("resolved", createModule("A"), createModule("B"));

        assertSame(declared, ProjectBean.chooseModulesSource(declared, resolved));
    }

    @Test
    void chooseModulesSourceShowsDefaultWildcardsWhenDeclaredHasNoModules() {
        var declared = createProject("declared");
        var resolved = createProject("resolved", createModule("A"), createModule("B"));

        var source = ProjectBean.chooseModulesSource(declared, resolved);

        // rules.xml declares no modules: seed the edit list with the default wildcard patterns, not the
        // expanded files, so edits keep the block intact.
        assertSame(declared, source);
        assertEquals(List.of("rules/**/*.xlsx", "tests/**/*.xlsx"),
                source.getModules().stream().map(Module::getRulesRootPath).toList());
    }

    @Test
    void chooseModulesSourceGivesAMutableModuleListForTheDeprecatedEditActions() {
        var declared = createProject("declared");

        var source = ProjectBean.chooseModulesSource(declared, createProject("resolved"));

        // The deprecated module-edit actions add to this list; it must not be the immutable default.
        source.getModules().add(createModule("rules/Extra.xlsx"));
        assertEquals(3, source.getModules().size());
    }

    @Test
    void chooseModulesSourceFallsBackToResolvedWhenDeclaredIsMissing() {
        var resolved = createProject("resolved", createModule("A"));

        assertSame(resolved, ProjectBean.chooseModulesSource(null, resolved));
    }

    @Test
    void applyDeclaredModulesReplacesTargetModulesWithDeclaredOnes() {
        var target = createProject("target", createModule("Expanded1"), createModule("Expanded2"));
        var declared = createProject("declared", createModule("rules/**/*.xlsx"));

        ProjectBean.applyDeclaredModules(target, declared);

        assertEquals(1, target.getModules().size());
        assertEquals("rules/**/*.xlsx", target.getModules().getFirst().getRulesRootPath());
    }

    @Test
    void applyDeclaredModulesClearsTargetWhenDeclaredHasNoModules() {
        var target = createProject("target", createModule("Expanded1"), createModule("Expanded2"));
        var declared = createProject("declared");

        ProjectBean.applyDeclaredModules(target, declared);

        assertTrue(target.getModules().isEmpty());
    }

    @Test
    void applyDeclaredModulesKeepsTargetWhenDeclaredIsMissing() {
        var target = createProject("target", createModule("Expanded1"), createModule("Expanded2"));

        ProjectBean.applyDeclaredModules(target, null);

        assertEquals(2, target.getModules().size());
    }

    @Test
    void applyDeclaredModulesRestoresWildcardWhenOpenApiGenerationAddedModules() {
        // OpenAPI file generation (createOrUpdateOpenAPISchema) clones the resolved descriptor - whose
        // wildcard is expanded into concrete files - and may append generated model/algorithm modules.
        // Saving must restore the declared wildcard so the <modules> block is not rewritten with the
        // expanded/generated files.
        var target = createProject("target",
                createModule("rules/A.xlsx"),
                createModule("rules/B.xlsx"),
                createModule("rules/Generated.xlsx"));
        var declared = createProject("declared", createModule("rules/*.xlsx"));

        ProjectBean.applyDeclaredModules(target, declared);

        assertEquals(1, target.getModules().size());
        assertEquals("rules/*.xlsx", target.getModules().getFirst().getRulesRootPath());
    }

    private static ProjectDescriptor createProject(String name, Module... modules) {
        var pd = new ProjectDescriptor();
        pd.setName(name);
        pd.setModules(List.of(modules));
        return pd;
    }

    private static Module createModule(String rulesRootPath) {
        var module = new Module();
        module.setName(rulesRootPath);
        module.setRulesRootPath(rulesRootPath);
        return module;
    }
}
