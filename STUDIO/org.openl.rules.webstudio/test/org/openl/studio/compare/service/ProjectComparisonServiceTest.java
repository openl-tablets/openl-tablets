package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.compare.model.ComparisonSideRequest;

class ProjectComparisonServiceTest {

    private static final String PROJECT = "Pricing";

    private final UserWorkspace workspace = mock(UserWorkspace.class);
    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final RulesProject project = mock(RulesProject.class);
    private final ProjectComparisonService service = spy(new ProjectComparisonService());

    ProjectComparisonServiceTest() {
        doReturn(workspace).when(service).getUserWorkspace();
        when(project.getArtefactPath()).thenReturn(new ArtefactPathImpl(PROJECT));
    }

    @Test
    void listsTheExcelFilesOfTheWorkingCopy() {
        // Every mock is built before any stubbing takes it, or Mockito reads one stubbing inside another.
        var rules = folder("rules", file("rules/Rates.xlsx"));
        var main = file("Main.xlsx");
        var descriptor = file("rules.xml");

        when(project.getArtefacts()).thenReturn(List.of(rules, main, descriptor));

        var files = service.excelFiles(project, null, null);

        // The project is read as its own user has it, and its own name is not part of the paths.
        assertEquals(List.of("Main.xlsx", "rules/Rates.xlsx"), files);
    }

    @Test
    void listsTheExcelFilesOfARevision() throws Exception {
        var revision = mock(AProject.class);
        var main = file("Main.xlsx");
        givenRevision("master", "rev-1", revision);
        when(revision.getArtefacts()).thenReturn(List.of(main));

        var files = service.excelFiles(project, "master", "rev-1");

        assertEquals(List.of("Main.xlsx"), files);
    }

    @Test
    void readsTheFileOfARevision() throws Exception {
        var revision = mock(AProject.class);
        var resource = file("rules/Rates.xlsx");
        var rules = folder("rules", resource);
        when(resource.getContent()).thenReturn(new ByteArrayInputStream(new byte[0]));
        givenRevision(null, "rev-1", revision);
        when(revision.getArtefacts()).thenReturn(List.of(rules));

        var content = service.read(project, new ComparisonSideRequest("rules/Rates.xlsx", null, "rev-1"));

        assertEquals("Rates.xlsx", content.name());
    }

    @Test
    void saysThatARevisionHasNoSuchFile() throws Exception {
        var revision = mock(AProject.class);
        var main = file("Main.xlsx");
        givenRevision(null, "rev-1", revision);
        when(revision.getArtefacts()).thenReturn(List.of(main));
        var side = new ComparisonSideRequest("rules/Rates.xlsx", null, "rev-1");

        assertThrows(NotFoundException.class, () -> service.read(project, side));
    }

    @Test
    void saysThatTheRepositoryHasNoSuchRevision() throws Exception {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn("DESIGN/rules/Pricing");
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(designTimeRepository.getProjectByPath("design", null, "DESIGN/rules/Pricing", "gone"))
                .thenThrow(new IllegalStateException("no such revision"));

        assertThrows(NotFoundException.class, () -> service.excelFiles(project, null, "gone"));
    }

    @Test
    void refusesAFileThatIsNotAWorkbook() {
        var side = new ComparisonSideRequest("rules.xml", null, null);

        assertThrows(BadRequestException.class, () -> service.read(project, side));
    }

    private void givenRevision(String branch, String revision, AProject read) throws Exception {
        when(read.getArtefactPath()).thenReturn(new ArtefactPathImpl(PROJECT));
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn("DESIGN/rules/Pricing");
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(designTimeRepository.getProjectByPath("design", branch, "DESIGN/rules/Pricing", revision))
                .thenReturn(read);
    }

    private static AProjectResource file(String path) {
        var artefact = mock(AProjectResource.class);
        when(artefact.getArtefactPath()).thenReturn(new ArtefactPathImpl(PROJECT + "/" + path));
        when(artefact.getName()).thenReturn(path.substring(path.lastIndexOf('/') + 1));
        return artefact;
    }

    private static AProjectFolder folder(String name, AProjectArtefact... artefacts) {
        var folder = mock(AProjectFolder.class);
        when(folder.getArtefacts()).thenReturn(List.of(artefacts));
        when(folder.getName()).thenReturn(name);
        return folder;
    }
}
