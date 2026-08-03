package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
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
    void migration_info_lists_the_root_workbooks_when_the_project_has_no_rules_xml() {
        rootFiles(file("Rating.xlsx"), file("Pricing.xlsx"), file("readme.txt"), folder("rules"));

        var info = service.migrationInfo(project);

        // Only root workbooks, sorted; the text file and the folder are left out.
        assertEquals(List.of("Pricing.xlsx", "Rating.xlsx"), info.rulesXml().movableRootModules());
        assertTrue(info.rulesXml().migratable());
        assertFalse(info.rulesDeploy().migratable());
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
        verify(filesService).createResource(eq(root), eq("rules.xml"), any(InputStream.class), eq(false));
        verify(filesService, never()).updateResource(any(), any(), any());
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

    private void rootFiles(FsNode... nodes) {
        when(filesService.getResources(eq(root), any(), eq(false), any(FileViewMode.class), any()))
                .thenReturn(List.of(nodes));
    }

    private void rulesXml(String xml) {
        rulesXmlBytes(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void rulesXmlBytes(byte[] bytes) {
        mockResource("rules.xml", bytes);
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
