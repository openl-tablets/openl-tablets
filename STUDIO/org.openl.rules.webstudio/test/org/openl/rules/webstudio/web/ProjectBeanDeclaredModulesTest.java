package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
    void applyDeclaredModulesRestoresWildcardOverTheExpandedFiles() {
        // OpenAPI file generation (createOrUpdateOpenAPISchema) clones the resolved descriptor, whose
        // wildcard is expanded into concrete files. Saving must restore the declared wildcard so the
        // <modules> block is not rewritten with those files.
        var target = createProject("target",
                createModule("rules/A.xlsx"),
                createModule("rules/B.xlsx"),
                createModule("rules/Generated.xlsx"));
        var declared = createProject("declared", createModule("rules/*.xlsx"));

        ProjectBean.applyDeclaredModules(target, declared);

        assertEquals(1, target.getModules().size());
        assertEquals("rules/*.xlsx", target.getModules().getFirst().getRulesRootPath());
    }

    @Test
    void generatedModulesAreDeclaredBesideTheOnesRulesXmlLists() {
        // The import wrote rules/Models.xlsx and named the module Models. rules.xml lists its modules one
        // by one, so nothing would find the new file unless the import declares it too.
        var target = createProject("target", createModule("Bank Rating", "rules/Bank Rating.xlsx"));
        var declared = createProject("declared", createModule("Bank Rating", "rules/Bank Rating.xlsx"));

        ProjectBean.applyDeclaredModules(target, declared, List.of(createModule("Models", "rules/Models.xlsx")));

        assertEquals(List.of("Bank Rating", "Models"), target.getModules().stream().map(Module::getName).toList());
    }

    @Test
    void aGeneratedModuleTheDeclaredWildcardAlreadyNamesStaysAutoDiscovered() {
        var target = createProject("target", createModule("rules/*.xlsx"));
        var declared = createProject("declared", createModule("rules/*.xlsx"));

        // The wildcard names the module after its workbook, which is the name the import asked for.
        ProjectBean.applyDeclaredModules(target, declared, List.of(createModule("Models", "rules/Models.xlsx")));

        assertEquals(1, target.getModules().size());
        assertEquals("rules/*.xlsx", target.getModules().getFirst().getRulesRootPath());
    }

    @Test
    void aGeneratedModuleKeepsTheNameItWasGivenUnderAWildcard() {
        var target = createProject("target", createModule("rules/*.xlsx"));
        var declared = createProject("declared", createModule("rules/*.xlsx"));

        // The wildcard would call it Models; the import was told to call it Data Types.
        ProjectBean.applyDeclaredModules(target, declared, List.of(createModule("Data Types", "rules/Models.xlsx")));

        assertEquals(List.of("rules/*.xlsx", "rules/Models.xlsx"),
                target.getModules().stream().map(Module::getRulesRootPath).toList());
        assertEquals("Data Types", target.getModules().getLast().getName());
    }

    @Test
    void aGeneratedModuleUnderTheDefaultsLeavesAnEmptyBlockEmpty() {
        var target = createProject("target", createModule("rules/Bank Rating.xlsx"));
        var declared = createProject("declared");

        // rules.xml declares nothing, so the project lives off the default wildcards - which cover the
        // generated file and name it as asked. Declaring it would hide every other module.
        ProjectBean.applyDeclaredModules(target, declared, List.of(createModule("Models", "rules/Models.xlsx")));

        assertTrue(target.getModules().isEmpty());
    }

    private static ProjectDescriptor createProject(String name, Module... modules) {
        var pd = new ProjectDescriptor();
        pd.setName(name);
        pd.setModules(new ArrayList<>(List.of(modules)));
        return pd;
    }

    private static Module createModule(String rulesRootPath) {
        return createModule(rulesRootPath, rulesRootPath);
    }

    private static Module createModule(String name, String rulesRootPath) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(rulesRootPath);
        return module;
    }
}
