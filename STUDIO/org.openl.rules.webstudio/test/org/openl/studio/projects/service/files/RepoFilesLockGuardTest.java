package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.ConflictException;

/**
 * A repository-mount modification of a path inside a project locked for editing by another user is
 * rejected before anything is written. The lock owner and paths outside any project are not
 * restricted. A full changeset affects the whole base folder subtree.
 *
 * @author Yury Molchan
 */
class RepoFilesLockGuardTest {

    private static final String REPO = "design-flat";
    private static final String BRANCH = "master";

    private Repository repository;
    private LockEngine lockEngine;
    private RepoFileRoot root;

    @BeforeEach
    void init() {
        repository = mock(Repository.class);
        lockEngine = mock(LockEngine.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        AProject project = mock(AProject.class);
        when(project.getRealPath()).thenReturn("rules/P1");
        when(project.getBusinessName()).thenReturn("P1");
        doReturn(List.of(project)).when(designTimeRepository).getProjects(REPO);
        var lockGuard = new ProjectLockGuard(designTimeRepository, lockEngine, REPO, BRANCH, "userB");
        root = new RepoFileRoot(repository, mock(AclProjectsHelper.class),
                mock(ProjectFileLookupService.class), lockGuard);
    }

    @Test
    void writeInsideProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedBy("userA");

        var ex = assertThrows(ConflictException.class,
                () -> root.writeBatch("rules", List.of(item("rules/P1/a.txt")), ChangesetType.DIFF, "Upload"));

        assertEquals("openl.error.409.file.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void lockOwnerIsNotRestricted() throws Exception {
        lockedBy("userB");

        root.writeBatch("rules", List.of(item("rules/P1/a.txt")), ChangesetType.DIFF, "Upload");

        verify(repository).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
    }

    @Test
    void unlockedProjectIsNotRestricted() throws Exception {
        when(lockEngine.getLockInfo(REPO, BRANCH, "rules/P1")).thenReturn(LockInfo.NO_LOCK);

        root.writeBatch("rules", List.of(item("rules/P1/a.txt")), ChangesetType.DIFF, "Upload");

        verify(repository).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
    }

    @Test
    void pathOutsideAnyProjectIsNotChecked() throws Exception {
        root.writeBatch("docs", List.of(item("docs/readme.txt")), ChangesetType.DIFF, "Upload");

        verify(repository).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        verify(lockEngine, never()).getLockInfo(any(), any(), any());
    }

    @Test
    void fullChangesetChecksTheWholeSubtree() throws Exception {
        lockedBy("userA");

        var ex = assertThrows(ConflictException.class,
                () -> root.writeBatch("", List.of(item("docs/readme.txt")), ChangesetType.FULL, "Replace"));

        assertEquals("openl.error.409.file.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void folderContainingTheProjectIsGuardedToo() {
        lockedBy("userA");

        var ex = assertThrows(ConflictException.class, () -> root.requireUnlocked(List.of("rules")));

        assertEquals("openl.error.409.file.project.locked.message", ex.getErrorCode());
    }

    @Test
    void singlePathOperationGuardsItsPath() {
        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        var service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class));
        RepoFileRoot mount = mock(RepoFileRoot.class);
        var tree = treeWithDataFile("a.txt");
        when(mount.readFolder(null)).thenReturn(tree);

        service.deleteResource(mount, "data/a.txt");

        verify(mount).requireUnlocked(List.of("data/a.txt"));
    }

    private void lockedBy(String userName) {
        LockInfo lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockInfo.getLockedBy()).thenReturn(userName);
        when(lockEngine.getLockInfo(REPO, BRANCH, "rules/P1")).thenReturn(lockInfo);
    }

    /**
     * Builds a mount tree with a "data" folder holding the given file, so the mount-relative path
     * of the file is "data/&lt;name&gt;".
     */
    private static AProjectFolder treeWithDataFile(String name) {
        AProject mountProject = mock(AProject.class);
        var mountData = new FileData();
        mountData.setName("");
        when(mountProject.getFileData()).thenReturn(mountData);
        var fileData = new FileData();
        fileData.setName("data/" + name);
        var dataFolder = new AProjectFolder(new HashMap<>(), mountProject, null, "data");
        dataFolder.addArtefact(new AProjectResource(mountProject, mock(Repository.class), fileData));
        var tree = new AProjectFolder(new HashMap<>(), mountProject, null, "");
        tree.addArtefact(dataFolder);
        return tree;
    }

    private static FileItem item(String name) {
        var data = new FileData();
        data.setName(name);
        return new FileItem(data, new ByteArrayInputStream(new byte[0]));
    }
}
