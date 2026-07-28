package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ForbiddenException;

class ProjectDescriptorCleanerTest {

    private static final String PROJECT_NAME = "TestProject";

    private ProjectDescriptorCleaner cleaner;
    private AclProjectsHelper aclProjectsHelper;
    private UserWorkspaceProject project;
    private AProjectResource descriptorResource;

    @BeforeEach
    void setUp() throws ProjectException {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        when(aclProjectsHelper.hasPermission(any(AProjectArtefact.class), eq(BasePermission.WRITE))).thenReturn(true);
        cleaner = new ProjectDescriptorCleaner(aclProjectsHelper);

        project = mock(UserWorkspaceProject.class);
        when(project.getName()).thenReturn(PROJECT_NAME);
        descriptorResource = mock(AProjectResource.class);
        when(descriptorResource.getArtefactPath())
                .thenReturn(new ArtefactPathImpl(PROJECT_NAME + "/" + ProjectDescriptor.FILE_NAME));
        when(project.getArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(descriptorResource);
    }

    @Test
    void deletingModuleFile_removesItsEntryFromDescriptor() throws Exception {
        givenDescriptor(descriptor(module("Main", "rules/Main.xlsx"), module("Other", "rules/Other.xlsx")));

        cleaner.unregisterModules(project, excelFile("rules/Main.xlsx"));

        var updated = writtenDescriptor();
        assertEquals(1, updated.getModules().size());
        assertEquals("Other", updated.getModules().get(0).getName());
    }

    @Test
    void deletingFolder_removesAllNestedModuleEntries() throws Exception {
        givenDescriptor(descriptor(module("Main", "rules/Main.xlsx"), module("Other", "rules/Other.xlsx")));

        List<AProjectArtefact> children = List.of(excelFile("rules/Main.xlsx"), excelFile("rules/Other.xlsx"));
        AProjectFolder folder = mock(AProjectFolder.class);
        when(folder.isFolder()).thenReturn(true);
        when(folder.getArtefactPath()).thenReturn(new ArtefactPathImpl(PROJECT_NAME + "/rules"));
        when(folder.getArtefacts()).thenReturn(children);

        cleaner.unregisterModules(project, folder);

        assertEquals(0, writtenDescriptor().getModules().size());
    }

    @Test
    void deletingUnmatchedFile_keepsDescriptorUntouched() throws Exception {
        givenDescriptor(descriptor(module("Main", "rules/Main.xlsx")));

        cleaner.unregisterModules(project, excelFile("rules/Unknown.xlsx"));

        verify(descriptorResource, never()).setContent(any());
    }

    @Test
    void missingDescriptor_isIgnored() throws Exception {
        when(project.getArtefact(ProjectDescriptor.FILE_NAME))
                .thenThrow(new ProjectException("Project has no rules.xml"));

        cleaner.unregisterModules(project, excelFile("rules/Main.xlsx"));

        verify(descriptorResource, never()).getContent();
    }

    @Test
    void deletingDescriptorItself_isIgnored() throws Exception {
        cleaner.unregisterModules(project, file(ProjectDescriptor.FILE_NAME));

        verify(descriptorResource, never()).getContent();
    }

    @Test
    void removedModule_clearsOpenApiModuleReferences() throws Exception {
        // A non-default OpenAPI path is required: a default RECONCILIATION block is dropped on serialization.
        ProjectDescriptor descriptor = descriptor(module("Main", "rules/Main.xlsx"), module("Other", "rules/Other.xlsx"));
        descriptor.setOpenapi(new OpenAPI("api/spec.json", OpenAPI.Mode.RECONCILIATION, "Other", "Main"));
        givenDescriptor(descriptor);

        cleaner.unregisterModules(project, excelFile("rules/Main.xlsx"));

        var updated = writtenDescriptor().getOpenapi();
        assertNull(updated.getAlgorithmModuleName());
        assertEquals("Other", updated.getModelModuleName());
    }

    @Test
    void deletingOpenApiFile_clearsOpenApiSection() throws Exception {
        ProjectDescriptor descriptor = descriptor(module("Main", "rules/Main.xlsx"));
        descriptor.setOpenapi(new OpenAPI("api/spec.json", OpenAPI.Mode.RECONCILIATION, null, null));
        givenDescriptor(descriptor);

        AProjectResource openApiFile = file("api/spec.json");
        var fileData = new FileData();
        fileData.setName("design/" + PROJECT_NAME + "/api/spec.json");
        when(openApiFile.getFileData()).thenReturn(fileData);

        cleaner.unregisterModules(project, openApiFile);

        assertNull(writtenDescriptor().getOpenapi());
    }

    @Test
    void withoutWritePermission_throwsForbiddenAndKeepsDescriptor() throws Exception {
        givenDescriptor(descriptor(module("Main", "rules/Main.xlsx")));
        when(aclProjectsHelper.hasPermission(eq(descriptorResource), eq(BasePermission.WRITE))).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> cleaner.unregisterModules(project, excelFile("rules/Main.xlsx")));

        verify(descriptorResource, never()).setContent(any());
    }

    // --- fixtures ---

    private void givenDescriptor(ProjectDescriptor descriptor) throws ProjectException {
        when(descriptorResource.getContent()).thenReturn(new ByteArrayInputStream(descriptor.toBytes()));
    }

    private ProjectDescriptor writtenDescriptor() throws ProjectException {
        var captor = ArgumentCaptor.forClass(InputStream.class);
        verify(descriptorResource).setContent(captor.capture());
        return ProjectDescriptor.read(captor.getValue());
    }

    private static ProjectDescriptor descriptor(Module... modules) {
        var descriptor = new ProjectDescriptor();
        descriptor.setName(PROJECT_NAME);
        descriptor.setModules(new ArrayList<>(List.of(modules)));
        return descriptor;
    }

    private static Module module(String name, String rulesRootPath) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(rulesRootPath);
        return module;
    }

    private static AProjectResource excelFile(String inProjectPath) {
        return file(inProjectPath);
    }

    private static AProjectResource file(String inProjectPath) {
        AProjectResource resource = mock(AProjectResource.class);
        when(resource.isFolder()).thenReturn(false);
        when(resource.getName()).thenReturn(inProjectPath.substring(inProjectPath.lastIndexOf('/') + 1));
        when(resource.getArtefactPath()).thenReturn(new ArtefactPathImpl(PROJECT_NAME + "/" + inProjectPath));
        return resource;
    }
}
