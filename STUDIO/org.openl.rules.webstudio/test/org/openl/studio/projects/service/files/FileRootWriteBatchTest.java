package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Verifies the atomic-write contract of {@link FileRoot}: the repository mount commits a batch as a
 * single changeset, while the workspace mount declares that it is written file by file.
 *
 * @author Yury Molchan
 */
class FileRootWriteBatchTest {

    private static FileItem item(String name) {
        var data = new FileData();
        data.setName(name);
        return new FileItem(data, new ByteArrayInputStream(new byte[0]));
    }

    @Test
    void repositoryMountCommitsBatchAsOneChangeset() throws Exception {
        BranchRepository repository = mock(BranchRepository.class);
        var root = new RepoFileRoot(repository, mock(AclProjectsHelper.class),
                mock(ProjectFileLookupService.class));
        var items = List.of(item("data/a.txt"), item("data/sub/b.txt"));

        assertTrue(root.supportsAtomicWrite());
        root.writeBatch(items, "Upload archive");

        var folder = ArgumentCaptor.forClass(FileData.class);
        verify(repository).save(folder.capture(), eq(items), eq(ChangesetType.DIFF));
        assertEquals("Upload archive", folder.getValue().getComment());
        assertEquals("", folder.getValue().getName());
    }

    @Test
    void emptyBatchDoesNotTouchRepository() {
        BranchRepository repository = mock(BranchRepository.class);
        var root = new RepoFileRoot(repository, mock(AclProjectsHelper.class),
                mock(ProjectFileLookupService.class));

        root.writeBatch(List.of(), "no-op");

        verifyNoInteractions(repository);
    }

    @Test
    void workspaceMountIsWrittenPerFile() {
        var root = new ProjectFileRoot(mock(RulesProject.class), mock(AclProjectsHelper.class),
                mock(ProjectStateValidator.class), mock(ProjectFileLookupService.class));

        assertFalse(root.supportsAtomicWrite());
        assertThrows(UnsupportedOperationException.class, () -> root.writeBatch(List.of(), "x"));
    }

    @Test
    void uploadArchiveToAtomicMountCommitsEveryEntryAsOneBatch() throws Exception {
        AclProjectsHelper acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        var service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class));

        var emptyTree = new AProjectFolder(new HashMap<>(), null, null, "");
        FileRoot root = mock(FileRoot.class);
        when(root.supportsAtomicWrite()).thenReturn(true);
        when(root.writeFolder()).thenReturn(emptyTree);
        when(root.readFolder(null)).thenReturn(emptyTree);

        byte[] archive = zip("a.txt", "AAA", "sub/b.txt", "BBB");
        service.uploadArchive(root, "data", new ByteArrayInputStream(archive), true, ConflictPolicy.FAIL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(items.capture(), eq("Upload archive to data"));
        assertEquals(2, items.getValue().size());
        assertEquals("data/a.txt", items.getValue().get(0).getData().getName());
    }

    @Test
    void uploadFilesToAtomicMountCommitsEveryFileAsOneBatch() {
        AclProjectsHelper acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        var service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class));

        var emptyTree = new AProjectFolder(new HashMap<>(), null, null, "");
        FileRoot root = mock(FileRoot.class);
        when(root.supportsAtomicWrite()).thenReturn(true);
        when(root.writeFolder()).thenReturn(emptyTree);
        when(root.readFolder(null)).thenReturn(emptyTree);

        var files = List.of(
                new ProjectFilesService.UploadedFile("x.txt", "X".getBytes(StandardCharsets.UTF_8)),
                new ProjectFilesService.UploadedFile("sub/y.txt", "Y".getBytes(StandardCharsets.UTF_8)));
        service.uploadFiles(root, "data", files, ConflictPolicy.FAIL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(items.capture(), eq("Upload files to data"));
        assertEquals(2, items.getValue().size());
        assertEquals("data/x.txt", items.getValue().get(0).getData().getName());
        assertEquals("data/sub/y.txt", items.getValue().get(1).getData().getName());
    }

    private static byte[] zip(String... nameThenContent) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(bytes)) {
            for (int i = 0; i < nameThenContent.length; i += 2) {
                zos.putNextEntry(new ZipEntry(nameThenContent[i]));
                zos.write(nameThenContent[i + 1].getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
