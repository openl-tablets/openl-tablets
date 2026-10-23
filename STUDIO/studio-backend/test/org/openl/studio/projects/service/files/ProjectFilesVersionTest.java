package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.PreconditionFailedException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.HashingUtils;

/**
 * A write of a file names the version of the file it is based on, and a write over a file changed since is refused:
 * nothing is written, so the change made since stays.
 */
class ProjectFilesVersionTest {

    private Repository repository;
    private ProjectFileRoot root;
    private ProjectFilesServiceImpl service;
    private AProjectResource file;

    @BeforeEach
    void init() throws IOException {
        repository = mock(Repository.class);
        var project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        when(project.isOpened()).thenReturn(true);

        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        root = new ProjectFileRoot(project, acl, mock(ProjectStateValidator.class),
                mock(ProjectFileLookupService.class), () -> new UserInfo("user1"), mock(DesignTimeRepository.class));
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()), new UploadFormatting(mock(TableFormatting.class)));

        var fileData = new FileData();
        fileData.setName("Project1/data.txt");
        file = new AProjectResource(project, repository, fileData);
        when(project.getArtefacts()).thenReturn(List.of(file));
        when(repository.read("Project1/data.txt")).thenAnswer(call -> new FileItem(fileData, content("on disk")));
        when(repository.save(any(FileData.class), any(InputStream.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void theVersionOfAFileIsADigestOfItsContent() throws IOException {
        assertEquals(HashingUtils.sha256Hex(content("on disk")), service.getVersion(file));
        assertNotEquals(HashingUtils.sha256Hex(content("read before")), service.getVersion(file));
    }

    @Test
    void aFileThatCannotBeReadHasNoVersion() throws IOException {
        when(repository.read("Project1/data.txt")).thenThrow(new IOException("gone"));

        var failed = assertThrows(IllegalStateException.class, () -> service.getVersion(file));

        assertEquals("Cannot read the file 'data.txt'.", failed.getMessage());
    }

    @Test
    void aWriteBasedOnAnotherVersionIsRefusedAndWritesNothing() throws IOException {
        var written = content("edited");
        var readBefore = HashingUtils.sha256Hex(content("read before"));

        var refused = assertThrows(PreconditionFailedException.class,
                () -> service.updateResource(root, "data.txt", written, readBefore));

        assertEquals("openl.error.412.file.changed.since.read.message", refused.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void aWriteBasedOnTheCurrentVersionIsWritten() throws IOException {
        var current = service.getVersion(file);

        assertDoesNotThrow(() -> service.updateResource(root, "data.txt", content("edited"), current));

        verify(repository).save(any(FileData.class), any(InputStream.class));
    }

    /** Of two writes based on the same version, the one that comes second is compared with what the first wrote. */
    @Test
    void ofTwoWritesBasedOnTheSameVersionTheSecondIsRefused() throws Exception {
        var onDisk = new AtomicReference<>("on disk");
        when(repository.read("Project1/data.txt"))
                .thenAnswer(call -> new FileItem(file.getFileData(), content(onDisk.get())));
        var saving = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(repository.save(any(FileData.class), any(InputStream.class))).thenAnswer(call -> {
            saving.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            onDisk.set(new String(call.<InputStream>getArgument(1).readAllBytes(), StandardCharsets.UTF_8));
            return call.getArgument(0);
        });
        var read = service.getVersion(file);
        var first = Thread.ofVirtual().start(() -> service.updateResource(root, "data.txt", content("first"), read));
        assertTrue(saving.await(10, TimeUnit.SECONDS));
        var refusal = new AtomicReference<Throwable>();
        var second = Thread.ofVirtual().start(() -> {
            try {
                service.updateResource(root, "data.txt", content("second"), read);
            } catch (RuntimeException e) {
                refusal.set(e);
            }
        });
        // The second write waits, either for the first one to end or, compared too early, in the save.
        while (second.getState() != Thread.State.WAITING && second.getState() != Thread.State.TIMED_WAITING) {
            Thread.onSpinWait();
        }

        release.countDown();
        first.join();
        second.join();

        assertInstanceOf(PreconditionFailedException.class, refusal.get());
        assertEquals("first", onDisk.get());
    }

    @Test
    void aDeletionBasedOnAnotherVersionIsRefusedAndDeletesNothing() throws IOException {
        var readBefore = HashingUtils.sha256Hex(content("read before"));

        assertThrows(PreconditionFailedException.class, () -> service.deleteResource(root, "data.txt", readBefore));

        verify(repository, never()).delete(any(FileData.class));
    }

    @Test
    void aWriteThatNamesNoVersionIsNotCompared() throws IOException {
        assertDoesNotThrow(() -> service.updateResource(root, "data.txt", content("edited")));
        assertDoesNotThrow(() -> service.deleteResource(root, "data.txt"));

        verify(repository).delete(any(FileData.class));
    }

    private static InputStream content(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
