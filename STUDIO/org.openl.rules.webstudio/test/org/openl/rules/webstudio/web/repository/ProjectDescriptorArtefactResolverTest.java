package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;

/**
 * Verifies that {@link ProjectDescriptorArtefactResolver#getLogicalName} never returns {@code null}.
 *
 * <p>The {@code name} declared in {@code rules.xml} is optional. When it is absent the resolver must
 * fall back to the physical project folder name instead of returning the descriptor's {@code null} name.
 *
 * @author Yury Molchan
 */
class ProjectDescriptorArtefactResolverTest {

    private static AProject mockProject(String rulesXml, String realPath) throws Exception {
        AProject project = mock(AProject.class);
        when(project.getFileData()).thenReturn(mock(FileData.class));
        when(project.getName()).thenReturn("physical-folder");
        when(project.getRealPath()).thenReturn(realPath);
        when(project.hasArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(true);
        AProjectResource resource = mock(AProjectResource.class);
        when(project.getArtefact(ProjectDescriptor.FILE_NAME)).thenReturn(resource);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream(rulesXml.getBytes(StandardCharsets.UTF_8)));
        return project;
    }

    @Test
    void usesDeclaredNameWhenPresent() throws Exception {
        var resolver = new ProjectDescriptorArtefactResolver();
        AProject project = mockProject("<project><name>Logical Name</name></project>", "DESIGN/physical-folder");
        assertEquals("Logical Name", resolver.getLogicalName(project));
    }

    @Test
    void fallsBackToFolderNameWhenNameIsAbsent() throws Exception {
        var resolver = new ProjectDescriptorArtefactResolver();
        AProject project = mockProject("<project/>", "DESIGN/some/path/physical-folder");
        assertEquals("physical-folder", resolver.getLogicalName(project));
    }
}
