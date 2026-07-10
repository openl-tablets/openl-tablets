package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Verifies the atomic-write contract of {@link FileRoot} and the upload changeset semantics: both
 * mounts commit a multi-file upload as a single changeset, and the {@code REPLACE} policy makes the
 * base folder contain exactly the upload via a {@code FULL} changeset.
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

        root.writeBatch("data", items, ChangesetType.DIFF, "Upload archive");

        var folder = ArgumentCaptor.forClass(FileData.class);
        verify(repository).save(folder.capture(), eq(items), eq(ChangesetType.DIFF));
        assertEquals("Upload archive", folder.getValue().getComment());
        assertEquals("data", folder.getValue().getName());
    }

    @Test
    void projectMountCommitsBatchThroughProjectRepository() throws Exception {
        BranchRepository repository = mock(BranchRepository.class);
        RulesProject project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        var root = new ProjectFileRoot(project, mock(AclProjectsHelper.class),
                mock(ProjectStateValidator.class), mock(ProjectFileLookupService.class));

        root.writeBatch("data", List.of(item("data/a.txt")), ChangesetType.FULL, "Replace data");

        var folder = ArgumentCaptor.forClass(FileData.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<FileItem>> items = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).save(folder.capture(), items.capture(), eq(ChangesetType.FULL));
        assertEquals("Project1/data", folder.getValue().getName());
        assertEquals("Replace data", folder.getValue().getComment());
        assertEquals("Project1/data/a.txt",
                ((List<FileItem>) items.getValue()).get(0).getData().getName());
        verify(project).refresh();
    }

    @Test
    void projectMountRootBatchTargetsTheProjectFolder() throws Exception {
        BranchRepository repository = mock(BranchRepository.class);
        RulesProject project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        var root = new ProjectFileRoot(project, mock(AclProjectsHelper.class),
                mock(ProjectStateValidator.class), mock(ProjectFileLookupService.class));

        root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files");

        var folder = ArgumentCaptor.forClass(FileData.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<FileItem>> items = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).save(folder.capture(), items.capture(), eq(ChangesetType.DIFF));
        assertEquals("Project1", folder.getValue().getName());
        assertEquals("Project1/a.txt", ((List<FileItem>) items.getValue()).get(0).getData().getName());
    }

    @Test
    void uploadArchiveCommitsEveryEntryAsOneBatch() throws Exception {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(emptyTree());

        byte[] archive = zip("a.txt", "AAA", "sub/b.txt", "BBB");
        service.uploadArchive(root, "data", new ByteArrayInputStream(archive), true, ConflictPolicy.FAIL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq("data"), items.capture(), eq(ChangesetType.DIFF), eq("Upload archive to data"));
        assertEquals(2, items.getValue().size());
        assertEquals("data/a.txt", items.getValue().get(0).getData().getName());
    }

    @Test
    void uploadFilesCommitsEveryFileAsOneBatch() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(emptyTree());

        var files = List.of(
                new ProjectFilesService.UploadedFile("x.txt", "X".getBytes(StandardCharsets.UTF_8)),
                new ProjectFilesService.UploadedFile("sub/y.txt", "Y".getBytes(StandardCharsets.UTF_8)));
        service.uploadFiles(root, "data", files, ConflictPolicy.FAIL);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq("data"), items.capture(), eq(ChangesetType.DIFF), eq("Upload files to data"));
        assertEquals(2, items.getValue().size());
        assertEquals("data/x.txt", items.getValue().get(0).getData().getName());
        assertEquals("data/sub/y.txt", items.getValue().get(1).getData().getName());
    }

    @Test
    void failPolicyRejectsAnExistingFile() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("keep.txt"));

        var files = List.of(new ProjectFilesService.UploadedFile("keep.txt", "K".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ConflictException.class,
                () -> service.uploadFiles(root, "data", files, ConflictPolicy.FAIL));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void skippedEntriesDoNotTouchTheMount() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("keep.txt"));

        var files = List.of(new ProjectFilesService.UploadedFile("keep.txt", "K".getBytes(StandardCharsets.UTF_8)));
        service.uploadFiles(root, "data", files, ConflictPolicy.SKIP);

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void replaceCommitsTheUploadAsAFullChangeset() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("keep.txt", "removed.txt"));

        var files = List.of(new ProjectFilesService.UploadedFile("keep.txt", "K".getBytes(StandardCharsets.UTF_8)));
        service.uploadFiles(root, "data", files, ConflictPolicy.REPLACE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq("data"), items.capture(), eq(ChangesetType.FULL), eq("Upload files to data"));
        assertEquals(1, items.getValue().size());
        assertEquals("data/keep.txt", items.getValue().get(0).getData().getName());
    }

    @Test
    void replaceRequiresDeletePermissionOnRemovedFiles() {
        AclProjectsHelper acl = grantAllAcl();
        var tree = treeWithDataFiles("keep.txt", "removed.txt");
        AProjectArtefact removed = findFile(tree, "removed.txt");
        when(acl.hasPermission(same(removed), eq(BasePermission.DELETE))).thenReturn(false);
        var service = service(acl);
        FileRoot root = mountOf(tree);

        var files = List.of(new ProjectFilesService.UploadedFile("keep.txt", "K".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ForbiddenException.class,
                () -> service.uploadFiles(root, "data", files, ConflictPolicy.REPLACE));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void replaceRejectsAnEmptyArchive() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("keep.txt"));

        // A valid archive with no entries: just the end-of-central-directory record.
        byte[] archive = new byte[22];
        archive[0] = 'P';
        archive[1] = 'K';
        archive[2] = 5;
        archive[3] = 6;
        assertThrows(BadRequestException.class,
                () -> service.uploadArchive(root, "data", new ByteArrayInputStream(archive), true,
                        ConflictPolicy.REPLACE));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    private static ProjectFilesServiceImpl service(AclProjectsHelper acl) {
        return new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class));
    }

    private static AclProjectsHelper grantAllAcl() {
        AclProjectsHelper acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        return acl;
    }

    private static FileRoot mountOf(AProjectFolder tree) {
        FileRoot root = mock(FileRoot.class);
        when(root.writeFolder()).thenReturn(tree);
        when(root.readFolder(null)).thenReturn(tree);
        return root;
    }

    private static AProjectFolder emptyTree() {
        return new AProjectFolder(new HashMap<>(), null, null, "");
    }

    /**
     * Builds a mount tree with a "data" folder holding the given files, rooted at a project whose
     * repository path is empty — so each file's mount-relative path is "data/&lt;name&gt;".
     */
    private static AProjectFolder treeWithDataFiles(String... names) {
        AProject mountProject = mock(AProject.class);
        var mountData = new FileData();
        mountData.setName("");
        when(mountProject.getFileData()).thenReturn(mountData);

        var dataFolder = new AProjectFolder(new HashMap<>(), mountProject, null, "data");
        for (String name : names) {
            var fileData = new FileData();
            fileData.setName("data/" + name);
            dataFolder.addArtefact(new AProjectResource(mountProject, null, fileData));
        }
        var tree = new AProjectFolder(new HashMap<>(), mountProject, null, "");
        tree.addArtefact(dataFolder);
        return tree;
    }

    private static AProjectArtefact findFile(AProjectFolder tree, String name) {
        try {
            return ((AProjectFolder) tree.getArtefact("data")).getArtefact(name);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
