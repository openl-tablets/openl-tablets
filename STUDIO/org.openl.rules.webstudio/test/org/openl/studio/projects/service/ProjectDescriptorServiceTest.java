package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.ProjectDescriptorView;

class ProjectDescriptorServiceTest {

    private final RepositoryAclService acl = mock(RepositoryAclService.class);
    private final ProjectDescriptorService service = new ProjectDescriptorService(acl);

    @Test
    void maps_descriptor_read_from_rules_xml() throws Exception {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("My Project");
        descriptor.setComment("hello");
        var module = new Module();
        module.setName("Main");
        module.setRulesRootPath("rules/Main.xlsx");
        var filter = new MethodFilter();
        filter.addIncludePattern("foo*");
        module.setMethodFilter(filter);
        descriptor.setModules(new ArrayList<>(List.of(module)));
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName("Common");
        dependency.setAutoIncluded(true);
        descriptor.setDependencies(new ArrayList<>(List.of(dependency)));
        descriptor.setClasspath(new ArrayList<>(List.of("lib/*.jar")));
        var exposed = new ExposedMethods();
        exposed.setIncludes(new LinkedHashSet<>(List.of("calc*")));
        descriptor.setExposedMethods(exposed);

        var project = stubProject(descriptor.toBytes(), true);

        ProjectDescriptorView view = service.getDescriptor(project);

        assertEquals("My Project", view.name());
        assertEquals("hello", view.comment());
        assertTrue(view.editable());
        assertFalse(view.contentHash().isBlank());
        assertEquals(1, view.modules().size());
        assertEquals("Main", view.modules().getFirst().name());
        assertEquals("rules/Main.xlsx", view.modules().getFirst().rulesRootPath());
        assertFalse(view.modules().getFirst().wildcard());
        assertEquals(List.of("foo*"), view.modules().getFirst().methodFilter().includes());
        assertEquals(1, view.dependencies().size());
        assertEquals("Common", view.dependencies().getFirst().name());
        assertTrue(view.dependencies().getFirst().autoIncluded());
        assertEquals(List.of("lib/*.jar"), view.classpath());
        assertEquals(1, view.exposedMethods().size());
        assertEquals("calc*", view.exposedMethods().getFirst().pattern());
        assertEquals("include", view.exposedMethods().getFirst().type());
    }

    @Test
    void update_splits_exposed_methods_into_includes_and_excludes() throws Exception {
        var project = stubProject(new ProjectDescriptor().toBytes(), true);
        var resource = (AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME);
        var hash = service.getDescriptor(project).contentHash();
        var exposed = List.of(
                new ProjectDescriptorView.ExposedMethodView("calc*", "include"),
                new ProjectDescriptorView.ExposedMethodView("legacy*", "exclude"));
        var model = new ProjectDescriptorView("Empty Project", null, null, null, null, null, exposed, null, null, true, hash);

        service.updateDescriptor(project, model, false);

        var captor = ArgumentCaptor.forClass(InputStream.class);
        verify(resource).setContent(captor.capture());
        var written = ProjectDescriptor.read(captor.getValue());
        assertEquals(Set.of("calc*"), written.getExposedMethods().getIncludes());
        assertEquals(Set.of("legacy*"), written.getExposedMethods().getExcludes());
    }

    @Test
    void marks_wildcard_modules() throws Exception {
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Wild");
        var module = new Module();
        module.setRulesRootPath("rules/**/*.xlsx");
        descriptor.setModules(new ArrayList<>(List.of(module)));

        var view = service.getDescriptor(stubProject(descriptor.toBytes(), true));

        assertTrue(view.modules().getFirst().wildcard());
    }

    @Test
    void returns_empty_editable_model_when_no_rules_xml() throws Exception {
        var project = mock(RulesProject.class);
        when(acl.isGranted(eq(project), anyList())).thenReturn(false);
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(false);

        ProjectDescriptorView view = service.getDescriptor(project);

        assertNull(view.name());
        assertFalse(view.editable());
        assertEquals("", view.contentHash());
        assertTrue(view.modules().isEmpty());
        assertTrue(view.dependencies().isEmpty());
        assertTrue(view.classpath().isEmpty());
    }

    @Test
    void update_writes_edited_descriptor_when_hash_matches() throws Exception {
        var current = new ProjectDescriptor();
        current.setName("Empty Project");
        var project = stubProject(current.toBytes(), true);
        var resource = (AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME);
        var hash = service.getDescriptor(project).contentHash();

        service.updateDescriptor(project, view(hash, "Empty Project", "A new comment"), false);

        var captor = ArgumentCaptor.forClass(InputStream.class);
        verify(resource).setContent(captor.capture());
        var written = ProjectDescriptor.read(captor.getValue());
        assertEquals("A new comment", written.getComment());
    }

    @Test
    void update_rejects_stale_hash_without_force() throws Exception {
        var project = stubProject(new ProjectDescriptor().toBytes(), true);

        assertThrows(ConflictException.class,
                () -> service.updateDescriptor(project, view("stale-hash", "Empty Project", "comment"), false));

        verify((AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME), never()).setContent(any());
    }

    @Test
    void update_overwrites_stale_hash_with_force() throws Exception {
        var project = stubProject(new ProjectDescriptor().toBytes(), true);
        var resource = (AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME);

        service.updateDescriptor(project, view("stale-hash", "Empty Project", "forced"), true);

        verify(resource).setContent(any());
    }

    @Test
    void update_forbidden_without_write_permission() throws Exception {
        var project = stubProject(new ProjectDescriptor().toBytes(), false);

        assertThrows(ForbiddenException.class,
                () -> service.updateDescriptor(project, view("any", "Empty Project", "comment"), false));

        verify((AProjectResource) project.getArtefact(ProjectDescriptor.FILE_NAME), never()).setContent(any());
    }

    private static ProjectDescriptorView view(String contentHash, String name, String comment) {
        return new ProjectDescriptorView(name, comment, null, null, null, null, null, null, null, true, contentHash);
    }

    private RulesProject stubProject(byte[] rulesXml, boolean writable) throws Exception {
        var project = mock(RulesProject.class);
        var resource = mock(AProjectResource.class);
        when(acl.isGranted(eq(project), anyList())).thenReturn(writable);
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        when(project.getArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(resource);
        when(resource.getContent()).thenAnswer(invocation -> new ByteArrayInputStream(rulesXml));
        return project;
    }
}
