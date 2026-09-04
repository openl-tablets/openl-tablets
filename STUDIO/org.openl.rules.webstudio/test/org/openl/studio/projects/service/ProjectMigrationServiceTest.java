package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.MigrationScope;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FolderNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;

class ProjectMigrationServiceTest {

    private final ProjectFilesService filesService = mock(ProjectFilesService.class);
    private final ProjectFileRootFactory fileRootFactory = mock(ProjectFileRootFactory.class);
    private final ProjectMigrationService service = new ProjectMigrationService(filesService, fileRootFactory);
    private final FileRoot root = mock(FileRoot.class);
    private final RulesProject project = mock(RulesProject.class);

    ProjectMigrationServiceTest() {
        when(fileRootFactory.of(project)).thenReturn(root);
        when(project.getBusinessName()).thenReturn("Pricing");
    }

    @Test
    void migration_info_lists_all_excel_root_workbooks_when_the_project_has_no_rules_xml() {
        rootFiles(file("Rating.xlsx"), file("Pricing.xlsx"), file("Legacy.xls"), file("readme.txt"), folder("rules"));

        var info = service.migrationInfo(project);

        // Every Excel workbook — .xls too — sorted; the text file and the folder are left out.
        assertEquals(List.of("Legacy.xls", "Pricing.xlsx", "Rating.xlsx"), info.rulesXml().movableRootModules());
        assertTrue(info.rulesXml().migratable());
        assertFalse(info.rulesDeploy().migratable());
    }

    @Test
    void migrate_moves_xls_and_xlsm_root_workbooks_too() {
        rootFiles(file("Main.xlsx"), file("Legacy.xls"), file("Macro.xlsm"), file("notes.txt"));

        service.migrate(project, MigrationScope.RULES_XML);

        // A .xls/.xlsm workbook is a module too, so the migrate moves it under rules/ instead of leaving it
        // behind where the rules/** default no longer finds it.
        verify(filesService).moveResource(root, "Main.xlsx", "rules/Main.xlsx");
        verify(filesService).moveResource(root, "Legacy.xls", "rules/Legacy.xls");
        verify(filesService).moveResource(root, "Macro.xlsm", "rules/Macro.xlsm");
        verify(filesService, never()).moveResource(root, "notes.txt", "rules/notes.txt");
        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).createResource(eq(root), eq("rules.xml"), written.capture(), eq(false));
        // .xls/.xlsm are not matched by the rules/** default, so every moved workbook is declared explicitly.
        assertEquals(List.of("rules/Legacy.xls", "rules/Macro.xlsm", "rules/Main.xlsx"),
                ProjectDescriptor.read(written.getValue()).getModules().stream()
                        .map(Module::getRulesRootPath).sorted().toList());
    }

    @Test
    void migration_info_flags_a_rules_xml_that_a_migrate_would_rewrite() {
        rootFiles(file("rules.xml"), file("Pricing.xlsx"));
        // A default classpath the migration drops — so migrating rewrites the file.
        rulesXml("""
                <project>
                    <name>Something</name>
                    <classpath>
                        <entry path="groovy/"/>
                        <entry path="lib/*.jar"/>
                    </classpath>
                </project>
                """);

        var info = service.migrationInfo(project);

        assertTrue(info.rulesXml().migratable());
        assertTrue(info.rulesXml().movableRootModules().isEmpty());
    }

    @Test
    void migration_info_reports_nothing_for_an_already_minimal_rules_xml() {
        var minimal = new ProjectDescriptor();
        minimal.setName("Other");
        rootFiles(file("rules.xml"));
        rulesXmlBytes(minimal.toBytes());

        var info = service.migrationInfo(project);

        assertFalse(info.rulesXml().migratable());
    }

    /** The descriptor of the test project, in every layout but the one Studio itself writes. */
    private static final String FORMATTED_BY_HAND = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- kept for the 2020 rate set -->
            <project>
              <name>Pricing</name>
            </project>
            """.replace("\n", "\r\n");

    @Test
    void migration_info_reports_nothing_for_a_rules_xml_that_only_differs_in_formatting() {
        rootFiles(file("rules.xml"), file("Pricing.xlsx"));
        rulesXml(FORMATTED_BY_HAND);

        var info = service.migrationInfo(project);

        // A prolog, a comment, CRLF line endings and an indent of its own are how the author wrote the file,
        // not legacy configuration — no migration would change what it declares.
        assertFalse(info.rulesXml().migratable());
    }

    @Test
    void migrate_leaves_a_rules_xml_that_only_differs_in_formatting_untouched() {
        rootFiles(file("rules.xml"), file("Pricing.xlsx"));
        rulesXml(FORMATTED_BY_HAND);

        service.migrate(project, MigrationScope.RULES_XML);

        // Rewriting it would serialize the descriptor anew and drop the comment for no gain.
        verify(filesService, never()).updateResource(any(), any(), any());
    }

    @Test
    void migration_info_reports_nothing_for_a_blank_value_the_serialization_drops_anyway() {
        rootFiles(file("rules.xml"));
        rulesXml("<project><name></name></project>");

        var info = service.migrationInfo(project);

        // An empty tag goes away whenever the descriptor is written — by Overview → Save as much as by a
        // migrate — so it is no reason of its own to rewrite the file.
        assertFalse(info.rulesXml().migratable());
    }

    @Test
    void migration_info_reports_nothing_for_a_rules_xml_that_cannot_be_read() {
        rootFiles(file("rules.xml"));
        rulesXml("<project><modules>");

        var info = service.migrationInfo(project);

        // Reading the info must not fail the screen it is read for.
        assertFalse(info.rulesXml().migratable());
    }

    @Test
    void migrate_refuses_a_rules_xml_that_cannot_be_read() {
        rootFiles(file("rules.xml"));
        rulesXml("<project><modules>");

        assertThrows(ConflictException.class, () -> service.migrate(project, MigrationScope.RULES_XML));

        verify(filesService, never()).updateResource(any(), any(), any());
    }

    @Test
    void migration_info_reports_nothing_for_a_rules_deploy_that_cannot_be_read() {
        rootFiles(file("rules-deploy.xml"));
        rulesDeploy("<rules-deploy><serviceName>");

        var info = service.migrationInfo(project);

        assertFalse(info.rulesDeploy().migratable());
    }

    @Test
    void migrate_refuses_a_rules_deploy_that_cannot_be_read() {
        rootFiles(file("rules-deploy.xml"));
        rulesDeploy("<rules-deploy><serviceName>");

        assertThrows(ConflictException.class, () -> service.migrate(project, MigrationScope.RULES_DEPLOY));

        verify(filesService, never()).updateResource(any(), any(), any());
    }

    @Test
    void migration_info_flags_a_rules_deploy_that_a_migrate_would_rewrite() {
        var minimal = new ProjectDescriptor();
        minimal.setName("Other");
        rootFiles(file("rules.xml"), file("rules-deploy.xml"));
        rulesXmlBytes(minimal.toBytes());
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);
        rulesDeployBytes(deploy.toBytes());

        var info = service.migrationInfo(project);

        // The rules-deploy carries the default isProvideRuntimeContext=false the migration drops.
        assertTrue(info.rulesDeploy().migratable());
        assertFalse(info.rulesXml().migratable());
    }

    @Test
    void migration_info_reports_nothing_when_there_is_no_rules_deploy() {
        rootFiles(file("rules.xml"));
        rulesXmlBytes(new ProjectDescriptor().toBytes());

        var info = service.migrationInfo(project);

        assertFalse(info.rulesDeploy().migratable());
    }

    @Test
    void migrate_moves_the_root_workbooks_and_writes_a_rules_xml() {
        rootFiles(file("Pricing.xlsx"), file("Rating.xlsx"));

        service.migrate(project, MigrationScope.RULES_XML);

        verify(filesService).moveResource(root, "Pricing.xlsx", "rules/Pricing.xlsx");
        verify(filesService).moveResource(root, "Rating.xlsx", "rules/Rating.xlsx");
        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).createResource(eq(root), eq("rules.xml"), written.capture(), eq(false));
        verify(filesService, never()).updateResource(any(), any(), any());
        // All workbooks are .xlsx, matched by the rules/** default, so the descriptor stays bare.
        assertTrue(ProjectDescriptor.read(written.getValue()).getModules().isEmpty());
    }

    @Test
    void migrate_does_nothing_when_there_are_no_root_workbooks_to_move() {
        // No rules.xml and no root .xlsx — matches migrationInfo()'s rulesXml.migratable == false, so
        // migrate() must not write a rules.xml either.
        rootFiles(file("readme.txt"));

        service.migrate(project, MigrationScope.RULES_XML);

        verify(filesService, never()).moveResource(any(), any(), any());
        verify(filesService, never()).createResource(any(), any(), any(), anyBoolean());
    }

    @Test
    void migrate_rewrites_an_existing_rules_xml_to_its_minimal_form() {
        rootFiles(file("rules.xml"), file("Pricing.xlsx"));
        rulesXml("""
                <project>
                    <name>Something</name>
                    <classpath>
                        <entry path="groovy/"/>
                        <entry path="lib/*.jar"/>
                    </classpath>
                </project>
                """);

        service.migrate(project, MigrationScope.RULES_XML);

        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).updateResource(eq(root), eq("rules.xml"), written.capture());
        verify(filesService, never()).moveResource(any(), any(), any());
        var migrated = ProjectDescriptor.read(written.getValue());
        // The default classpath is dropped by the migration (an absent classpath reads back as empty).
        assertTrue(migrated.getClasspath().isEmpty());
    }

    @Test
    void migrate_keeps_a_project_name_even_when_it_equals_the_folder() {
        rootFiles(file("rules.xml"), file("Pricing.xlsx"));
        // The name equals the project (folder) name; the default classpath still makes the file migratable.
        rulesXml("""
                <project>
                    <name>Pricing</name>
                    <classpath>
                        <entry path="groovy/"/>
                    </classpath>
                </project>
                """);

        service.migrate(project, MigrationScope.RULES_XML);

        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).updateResource(eq(root), eq("rules.xml"), written.capture());
        var migrated = ProjectDescriptor.read(written.getValue());
        // Studio never drops the project name — unlike the Maven goal, which drops one equal to the folder.
        assertEquals("Pricing", migrated.getName());
    }

    @Test
    void migrate_rewrites_the_rules_deploy_and_leaves_the_rules_xml_alone() {
        rootFiles(file("rules-deploy.xml"));
        var deploy = new RulesDeploy();
        deploy.setProvideRuntimeContext(false);
        rulesDeployBytes(deploy.toBytes());

        service.migrate(project, MigrationScope.RULES_DEPLOY);

        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).updateResource(eq(root), eq("rules-deploy.xml"), written.capture());
        verify(filesService, never()).updateResource(eq(root), eq("rules.xml"), any());
        verify(filesService, never()).moveResource(any(), any(), any());
        var migrated = RulesDeploy.read(written.getValue());
        assertNull(migrated.isProvideRuntimeContext());
    }

    @Test
    void migrate_rules_deploy_does_nothing_when_the_project_has_none() {
        rootFiles(file("rules.xml"));

        service.migrate(project, MigrationScope.RULES_DEPLOY);

        verify(filesService, never()).updateResource(any(), any(), any());
    }

    @Test
    void migration_info_lists_the_workbooks_a_widening_rewrite_would_expose() {
        rootFiles(file("rules.xml"), file("rules/Main.xlsx"), file("rules/Extra.xlsx"), file("tests/Cases.xlsx"));
        rulesXml("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);

        var info = service.migrationInfo(project);

        // Dropping <modules> restores the rules/** and tests/** defaults, exposing the undeclared workbooks
        // in both folders — the guard is not limited to rules/.
        assertEquals(List.of("rules/Extra.xlsx", "tests/Cases.xlsx"), info.rulesXml().newModules());
        assertTrue(info.rulesXml().migratable());
    }

    @Test
    void migrate_refuses_a_rewrite_that_would_turn_an_undeclared_workbook_into_a_module() {
        rootFiles(file("rules.xml"), file("rules/Main.xlsx"), file("rules/Extra.xlsx"));
        // Only Main is a module; collapsing it to rules/**/*.xlsx would pull in the undeclared Extra.
        rulesXml("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);

        assertThrows(ConflictException.class, () -> service.migrate(project, MigrationScope.RULES_XML));

        verify(filesService, never()).updateResource(any(), any(), any());
        verify(filesService, never()).moveResource(any(), any(), any());
    }

    @Test
    void migrate_rewrites_and_keeps_a_config_carrying_module_without_widening() {
        rootFiles(file("rules.xml"), file("rules/Main.xlsx"), file("rules/Extra.xlsx"));
        // A default classpath makes the file migratable; the module carries compileThisModuleOnly, and Extra
        // sits undeclared next to it. The module stays explicit, so Extra is not pulled in and nothing widens.
        rulesXml("""
                <project>
                    <classpath>
                        <entry path="lib/*.jar"/>
                    </classpath>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                            <webstudioConfiguration>
                                <compileThisModuleOnly>true</compileThisModuleOnly>
                            </webstudioConfiguration>
                        </module>
                    </modules>
                </project>
                """);

        service.migrate(project, MigrationScope.RULES_XML);

        var written = ArgumentCaptor.forClass(InputStream.class);
        verify(filesService).updateResource(eq(root), eq("rules.xml"), written.capture());
        var migrated = ProjectDescriptor.read(written.getValue());
        assertTrue(migrated.getClasspath().isEmpty());
        assertEquals(List.of("rules/Main.xlsx"),
                migrated.getModules().stream().map(Module::getRulesRootPath).toList());
        assertTrue(migrated.getModules().get(0).getWebstudioConfiguration().isCompileThisModuleOnly());
    }

    @Test
    void migrate_ignores_temp_lock_files_when_checking_for_widening() {
        rootFiles(file("rules.xml"), file("rules/Main.xlsx"), file("rules/~$Main.xlsx"));
        // The ~$ lock file matches rules/**/*.xlsx by name but is not a workbook, so collapsing Main.xlsx to
        // the folder wildcard exposes no new module and the migrate must not be refused.
        rulesXml("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="rules/Main.xlsx"/>
                        </module>
                    </modules>
                </project>
                """);

        service.migrate(project, MigrationScope.RULES_XML);

        verify(filesService).updateResource(eq(root), eq("rules.xml"), any());
    }

    private void rootFiles(FsNode... nodes) {
        // Stub both the root-level listing (recursive=false) and the recursive one the widening check uses.
        when(filesService.getResources(eq(root), any(), anyBoolean(), any(FileViewMode.class), any()))
                .thenReturn(List.of(nodes));
    }

    private void rulesXml(String xml) {
        rulesXmlBytes(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void rulesXmlBytes(byte[] bytes) {
        mockResource("rules.xml", bytes);
    }

    private void rulesDeploy(String xml) {
        rulesDeployBytes(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void rulesDeployBytes(byte[] bytes) {
        mockResource("rules-deploy.xml", bytes);
    }

    private void mockResource(String name, byte[] bytes) {
        var resource = mock(AProjectResource.class);
        try {
            when(resource.getContent()).thenReturn(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        when(filesService.getResource(root, name, null)).thenReturn(resource);
    }

    private static FileNode file(String name) {
        return FileNode.builder().path(name).name(name).build();
    }

    private static FolderNode folder(String name) {
        return FolderNode.builder().path(name).name(name).build();
    }
}
