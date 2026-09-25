package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * A modification committed directly to a closed project answers only once the project index publishes it, so the
 * next read of the project sees it. An opened project and a repository without branches have nothing to wait for.
 *
 * @author Yury Molchan
 */
class ProjectFilesIndexWaitTest {

    private BranchRepository repository;
    private RulesProject project;
    private DesignTimeRepository designTimeRepository;
    private ProjectFileRoot root;
    private ProjectFilesServiceImpl service;

    @BeforeEach
    void init() {
        repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        when(project.getBranch()).thenReturn("feature");
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.isLockedByMe()).thenReturn(true);

        designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        var stateValidator = mock(ProjectStateValidator.class);
        when(stateValidator.canModify(project)).thenReturn(true);
        root = new ProjectFileRoot(project, acl, stateValidator, mock(ProjectFileLookupService.class),
                () -> new UserInfo("user1"), designTimeRepository);
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class), new BeanValidationProvider(List.of()));
    }

    @Test
    void deletionInClosedProjectWaitsForTheIndexBeforeReleasingTheLock() throws Exception {
        projectWithFile("data.txt");

        service.deleteResource(root, "data.txt");

        var order = inOrder(repository, designTimeRepository, project);
        order.verify(repository).delete(any(FileData.class));
        order.verify(designTimeRepository).refreshBranch("design", "feature");
        order.verify(project).unlock();
    }

    @Test
    void uploadToClosedProjectWaitsForTheIndex() throws Exception {
        root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files");

        var order = inOrder(repository, designTimeRepository, project);
        order.verify(repository).save(any(FileData.class), any(), any());
        order.verify(designTimeRepository).refreshBranch("design", "feature");
        order.verify(project).unlock();
    }

    @Test
    void closedProjectReportsAnUnpublishedCommitAndReleasesTheLock() throws Exception {
        projectWithFile("data.txt");
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("The index is down")));

        var ex = assertThrows(ConflictException.class, () -> service.deleteResource(root, "data.txt"));

        assertEquals("openl.error.409.project.indexing.incomplete.message", ex.getErrorCode());
        verify(repository).delete(any(FileData.class));
        verify(project).unlock();
    }

    @Test
    void openedProjectWaitsForNothing() {
        when(project.isOpened()).thenReturn(true);
        projectWithFile("data.txt");

        service.updateResource(root, "data.txt", new ByteArrayInputStream(new byte[0]));

        verifyNoInteractions(designTimeRepository);
    }

    @Test
    void repositoryWithoutBranchesWaitsForNothing() {
        when(project.isSupportsBranches()).thenReturn(false);

        service.createResource(root, "new.txt", new ByteArrayInputStream(new byte[0]), false);

        verify(designTimeRepository, never()).refreshBranch(anyString(), anyString());
        verify(designTimeRepository, never()).refresh();
    }

    @Test
    void folderCreationInClosedProjectWaitsForNothing() {
        // A folder is kept in memory until a file lands in it, so creating one commits nothing.
        service.createFolder(root, "folder", true);

        verifyNoInteractions(designTimeRepository);
    }

    private void projectWithFile(String name) {
        var fileData = new FileData();
        fileData.setName("Project1/" + name);
        var resource = new AProjectResource(project, repository, fileData);
        when(project.getArtefacts()).thenReturn(List.of(resource));
    }

    private static FileItem item(String name) {
        var data = new FileData();
        data.setName(name);
        return new FileItem(data, new ByteArrayInputStream(new byte[0]));
    }
}
