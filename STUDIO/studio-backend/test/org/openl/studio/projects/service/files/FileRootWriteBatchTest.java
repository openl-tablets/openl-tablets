package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Verifies the atomic-write contract of {@link FileRoot} and the upload changeset semantics: both
 * mounts commit a multi-file upload as a single changeset, and the {@code REPLACE} policy makes the
 * base folder contain exactly the upload via a {@code FULL} changeset. A write of several files is
 * checked file by file before its one changeset is written.
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
                mock(ProjectFileLookupService.class), mock(ProjectLockGuard.class), mock(DesignTimeRepository.class),
                null);
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
        RulesProject project = projectIn(repository);
        var author = new UserInfo("user1");
        var root = projectMount(project, author);

        root.writeBatch("data", List.of(item("data/a.txt")), ChangesetType.FULL, "Replace data");

        var folder = ArgumentCaptor.forClass(FileData.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<FileItem>> items = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).save(folder.capture(), items.capture(), eq(ChangesetType.FULL));
        assertEquals("Project1/data", folder.getValue().getName());
        assertEquals("Replace data", folder.getValue().getComment());
        assertEquals(author, folder.getValue().getAuthor());
        assertEquals("Project1/data/a.txt",
                ((List<FileItem>) items.getValue()).getFirst().getData().getName());
        verify(project).refresh();
    }

    @Test
    void projectMountRootBatchTargetsTheProjectFolder() throws Exception {
        BranchRepository repository = mock(BranchRepository.class);
        var root = projectMount(projectIn(repository), new UserInfo("user1"));

        root.writeBatch("", List.of(item("a.txt")), ChangesetType.DIFF, "Upload files");

        var folder = ArgumentCaptor.forClass(FileData.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<FileItem>> items = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).save(folder.capture(), items.capture(), eq(ChangesetType.DIFF));
        assertEquals("Project1", folder.getValue().getName());
        assertEquals("Project1/a.txt", ((List<FileItem>) items.getValue()).getFirst().getData().getName());
    }

    @Test
    void projectMountKeepsWhyTheRepositoryRefusedTheBatch() throws Exception {
        BranchRepository repository = mock(BranchRepository.class);
        var refusal = new IOException("Commit author name is blank.");
        when(repository.save(any(FileData.class), any(), any())).thenThrow(refusal);
        var root = projectMount(projectIn(repository), new UserInfo("user1"));
        var items = List.of(item("a.txt"));

        var conflict = assertThrows(ConflictException.class,
                () -> root.writeBatch("", items, ChangesetType.DIFF, "Upload files"));

        assertEquals("openl.error.409.file.archive.upload.failed.message", conflict.getErrorCode());
        assertSame(refusal, conflict.getCause());
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
        assertEquals("data/a.txt", items.getValue().getFirst().getData().getName());
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
        assertEquals("data/x.txt", items.getValue().getFirst().getData().getName());
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
        assertEquals("data/keep.txt", items.getValue().getFirst().getData().getName());
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
        var content = new ByteArrayInputStream(archive);
        assertThrows(BadRequestException.class,
                () -> service.uploadArchive(root, "data", content, true, ConflictPolicy.REPLACE));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void writeFilesCommitsEveryFileAsOneBatch() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("kept.txt"));

        service.writeFiles(root, written("data/kept.txt", "data/sub/added.txt"), List.of(), "Generate tables");

        var items = batchOf(root, "Generate tables");
        assertEquals(List.of("data/kept.txt", "data/sub/added.txt"),
                items.stream().map(item -> item.getData().getName()).toList());
    }

    @Test
    void writeFilesDeletesEveryFileOfAFolderItDoesNotWriteAgain() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("kept.txt", "removed.txt"));

        service.writeFiles(root, written("data/kept.txt"), List.of("data", "missing"), "Generate tables");

        // The file written again is replaced rather than deleted, and a path holding nothing deletes nothing.
        var items = batchOf(root, "Generate tables");
        assertEquals(2, items.size());
        assertEquals("data/kept.txt", items.getFirst().getData().getName());
        assertNotNull(items.getFirst().getStream());
        assertEquals("data/removed.txt", items.get(1).getData().getName());
        assertNull(items.get(1).getStream());
    }

    @Test
    void writeFilesWritesNothingWhenOneFileMayNotBeWritten() {
        AclProjectsHelper acl = grantAllAcl();
        var tree = treeWithDataFiles("kept.txt");
        when(acl.hasPermission(same(findFile(tree, "kept.txt")), eq(BasePermission.WRITE))).thenReturn(false);
        var service = service(acl);
        FileRoot root = mountOf(tree);
        var files = written("added.txt", "data/kept.txt");

        assertThrows(ForbiddenException.class, () -> service.writeFiles(root, files, List.of(), "Generate tables"));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void writeFilesWritesNothingWhenOneFileMayNotBeDeleted() {
        AclProjectsHelper acl = grantAllAcl();
        var tree = treeWithDataFiles("removed.txt");
        when(acl.hasPermission(same(findFile(tree, "removed.txt")), eq(BasePermission.DELETE))).thenReturn(false);
        var service = service(acl);
        FileRoot root = mountOf(tree);
        var files = written("added.txt");
        var deleted = List.of("data");

        assertThrows(ForbiddenException.class, () -> service.writeFiles(root, files, deleted, "Generate tables"));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void writeFilesRefusesAFileWhereAFolderOrAFileStandsInTheWay() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(treeWithDataFiles("kept.txt"));
        var overFolder = written("data");
        var underFile = written("data/kept.txt/nested.txt");
        List<String> nothing = List.of();

        assertThrows(ConflictException.class, () -> service.writeFiles(root, overFolder, nothing, "Generate tables"));
        assertThrows(ConflictException.class, () -> service.writeFiles(root, underFile, nothing, "Generate tables"));

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    @Test
    void writeFilesWithNothingToChangeTouchesNothing() {
        var service = service(grantAllAcl());
        FileRoot root = mountOf(emptyTree());

        service.writeFiles(root, written(), List.of("missing"), "Generate tables");

        verify(root, never()).writeBatch(any(), any(), any(), any());
    }

    /** A project named Project1 kept in the given repository. */
    private static RulesProject projectIn(BranchRepository repository) {
        RulesProject project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        return project;
    }

    private static ProjectFileRoot projectMount(RulesProject project, UserInfo author) {
        return new ProjectFileRoot(project, mock(AclProjectsHelper.class), mock(ProjectStateValidator.class),
                mock(ProjectFileLookupService.class), () -> author, mock(DesignTimeRepository.class));
    }

    /** Files of the given paths, each holding its own path as text. */
    private static Map<String, byte[]> written(String... paths) {
        var files = new LinkedHashMap<String, byte[]>();
        for (var path : paths) {
            files.put(path, path.getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }

    /** The one batch the mount was written with, at its root. */
    private static List<FileItem> batchOf(FileRoot root, String comment) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq(""), items.capture(), eq(ChangesetType.DIFF), eq(comment));
        return items.getValue();
    }

    private static ProjectFilesServiceImpl service(AclProjectsHelper acl) {
        return new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()));
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

    /** An archive holding the given entries, each name followed by its text. */
    static byte[] zip(String... nameThenContent) throws IOException {
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
