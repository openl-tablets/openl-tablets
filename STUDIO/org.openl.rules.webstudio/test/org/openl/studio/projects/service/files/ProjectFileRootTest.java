package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * The files of a closed project are written the way the project is kept.
 *
 * <p>A project kept in folders commits each written file as the current user. A project kept as an archive
 * reads and writes its files through the archive.
 *
 * @author Yury Molchan
 */
class ProjectFileRootTest {

    private BranchRepository design;
    private RulesProject project;
    private ProjectFileRoot root;

    @BeforeEach
    void setUp() {
        design = mock(BranchRepository.class);
        project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(design);
        when(project.getFolderPath()).thenReturn("Project1");
        root = new ProjectFileRoot(project, mock(AclProjectsHelper.class), mock(ProjectStateValidator.class),
                mock(ProjectFileLookupService.class), () -> new UserInfo("user1"), mock(DesignTimeRepository.class));
    }

    @Test
    void fileOfAProjectKeptInFoldersIsWrittenAsTheCurrentUser() throws Exception {
        when(project.isFolder()).thenReturn(true);
        when(project.getFileData()).thenReturn(named("Project1"));
        var data = named("Project1/data.txt");
        data.setAuthor(new UserInfo("previous"));
        data.setComment("Add the project");
        when(project.getArtefacts()).thenReturn(List.of(new AProjectResource(project, design, data)));

        var file = (AProjectResource) root.readFolder(null).getArtefact("data.txt");
        file.setContent(InputStream.nullInputStream());

        var saved = ArgumentCaptor.forClass(FileData.class);
        verify(design).save(saved.capture(), any(InputStream.class));
        assertEquals("user1", saved.getValue().getAuthor().getName());
        assertEquals("Save data.txt", saved.getValue().getComment());
        // The mount is known by the data of the project, so permissions are asked about the same project.
        assertSame(project.getFileData(), root.writeFolder().getFileData());
    }

    @Test
    void projectWhoseRevisionCannotBeFoundIsStillRead() throws Exception {
        when(project.isFolder()).thenReturn(true);
        var file = new AProjectResource(project, design, named("Project1/data.txt"));
        when(project.getArtefacts()).thenReturn(List.of(file));

        var read = root.readFolder(null).getArtefact("data.txt");

        assertSame(file.getFileData(), read.getFileData());
    }

    @Test
    void folderHeldInMemoryIsWrittenAsTheCurrentUser() throws Exception {
        when(project.isFolder()).thenReturn(true);
        when(project.getArtefacts()).thenReturn(List.of(new AProjectFolder(project, design, "Project1/docs", null)));

        var folder = root.readFolder(null).getArtefact("docs");

        assertInstanceOf(AuthoringRepository.class, folder.getRepository());
    }

    @Test
    void fileOfAProjectKeptAsAnArchiveStaysInTheArchive() throws Exception {
        var archive = mock(Repository.class);
        var file = new AProjectResource(project, archive, named("Project1/rules.xml"));
        when(project.getArtefacts()).thenReturn(List.of(file));

        assertSame(file, root.readFolder(null).getArtefact("rules.xml"));
        assertSame(project, root.writeFolder());
    }

    private static FileData named(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }
}
