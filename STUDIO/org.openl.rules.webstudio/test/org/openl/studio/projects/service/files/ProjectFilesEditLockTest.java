package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Every modification of a project mount must reserve the project for editing, exactly as table
 * edits do, and report a lock conflict precisely. A project locked by another user rejects the
 * modification with the dedicated conflict and stays untouched. A repository mount needs no lock.
 *
 * @author Yury Molchan
 */
class ProjectFilesEditLockTest {

    private Repository repository;
    private RulesProject project;
    private ProjectFileRoot root;
    private ProjectDescriptorCleaner descriptorCleaner;
    private ProjectFilesServiceImpl service;

    @BeforeEach
    void init() {
        repository = mock(Repository.class);
        project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        when(project.isOpened()).thenReturn(true);

        AclProjectsHelper acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        ProjectStateValidator stateValidator = mock(ProjectStateValidator.class);
        when(stateValidator.canModify(project)).thenReturn(true);
        root = new ProjectFileRoot(project, acl, stateValidator, mock(ProjectFileLookupService.class),
                () -> new UserInfo("user1"));

        descriptorCleaner = mock(ProjectDescriptorCleaner.class);
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), descriptorCleaner);
    }

    @Test
    void uploadToProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();

        var ex = assertThrows(ConflictException.class,
                () -> root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files"));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void uploadLocksTheProjectForEditing() throws Exception {
        root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files");

        verify(project).tryLockOrThrow();
        verify(repository).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        verify(project, never()).unlock();
    }

    @Test
    void uploadToClosedProjectDoesNotLeaveTheLock() throws Exception {
        closedProject();

        root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files");

        verify(project).tryLockOrThrow();
        verify(repository).save(any(FileData.class), any(), eq(ChangesetType.DIFF));
        verify(project).unlock();
    }

    @Test
    void failedUploadToClosedProjectDoesNotLeaveTheLock() throws Exception {
        closedProject();
        doThrow(new IOException()).when(repository).save(any(FileData.class), any(), any());

        assertThrows(ConflictException.class,
                () -> root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files"));

        verify(project).unlock();
    }

    @Test
    void lockOfAnotherUserOnClosedProjectIsNeverTouched() throws Exception {
        when(project.isOpened()).thenReturn(false);
        lockedByAnotherUser();

        assertThrows(ConflictException.class,
                () -> root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files"));

        verify(project, never()).unlock();
    }

    @Test
    void deletionInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();
        projectWithFile("data.txt");

        var ex = assertThrows(ConflictException.class, () -> service.deleteResource(root, "data.txt"));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).delete(any(FileData.class));
        verify(descriptorCleaner, never()).unregisterModules(any(), any());
    }

    @Test
    void deletionLocksTheProjectForEditing() throws Exception {
        projectWithFile("data.txt");

        assertDoesNotThrow(() -> service.deleteResource(root, "data.txt"));

        verify(project).tryLockOrThrow();
        verify(repository).delete(any(FileData.class));
        verify(project, never()).unlock();
    }

    @Test
    void deletionInClosedProjectDoesNotLeaveTheLock() throws Exception {
        closedProject();
        projectWithFile("data.txt");

        assertDoesNotThrow(() -> service.deleteResource(root, "data.txt"));

        verify(repository).delete(any(FileData.class));
        verify(project).unlock();
    }

    @Test
    void closedProjectIsReservedBeforeResolutionAndReleasedOnFailure() throws Exception {
        closedProject();

        assertThrows(NotFoundException.class,
                () -> service.updateResource(root, "missing.txt", new ByteArrayInputStream(new byte[0])));

        verify(project).tryLockOrThrow();
        verify(project).unlock();
    }

    @Test
    void openedProjectIsNotLockedByARejectedRequest() throws Exception {
        assertThrows(NotFoundException.class,
                () -> service.updateResource(root, "missing.txt", new ByteArrayInputStream(new byte[0])));

        verify(project, never()).tryLockOrThrow();
        verify(project, never()).unlock();
    }

    @Test
    void updateInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();
        projectWithFile("data.txt");

        var ex = assertThrows(ConflictException.class,
                () -> service.updateResource(root, "data.txt", new ByteArrayInputStream(new byte[0])));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void creationInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();

        var ex = assertThrows(ConflictException.class,
                () -> service.createResource(root, "new.txt", new ByteArrayInputStream(new byte[0]), false));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void folderCreationInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();

        var ex = assertThrows(ConflictException.class, () -> service.createFolder(root, "folder", true));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
    }

    @Test
    void copyInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();
        projectWithFile("data.txt");

        var ex = assertThrows(ConflictException.class,
                () -> service.copyResource(root, "data.txt", "copy.txt"));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void moveInProjectLockedByAnotherUserIsRejected() throws Exception {
        lockedByAnotherUser();
        projectWithFile("data.txt");

        var ex = assertThrows(ConflictException.class,
                () -> service.moveResource(root, "data.txt", "moved.txt"));

        assertEquals("openl.error.409.project.locked.message", ex.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
        verify(repository, never()).delete(any(FileData.class));
    }

    @Test
    void repositoryMountNeedsNoLock() {
        FileRoot repoMount = mock(FileRoot.class);
        var tree = new AProjectFolder(new HashMap<>(), mock(AProject.class), null, "");
        when(repoMount.writeFolder()).thenReturn(tree);
        when(repoMount.readFolder(null)).thenReturn(tree);

        assertDoesNotThrow(() -> service.createFolder(repoMount, "folder", true));
    }

    private void lockedByAnotherUser() throws ProjectException {
        doThrow(new ProjectException("The project is locked by other user")).when(project).tryLockOrThrow();
    }

    private void closedProject() {
        when(project.isOpened()).thenReturn(false);
        when(project.isLockedByMe()).thenReturn(true);
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
