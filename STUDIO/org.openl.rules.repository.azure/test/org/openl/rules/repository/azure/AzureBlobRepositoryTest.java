package org.openl.rules.repository.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.IOUtils;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.specialized.BlobInputStream;

public class AzureBlobRepositoryTest {
    private AzureBlobRepository repo;
    private final Map<String, List<BlobEmulation>> blobs = new HashMap<>();

    @Before
    public void setUp() throws IOException {
        BlobContainerClient client = mockContainerClient();

        repo = new AzureBlobRepository();
        repo.setBlobContainerClient(client);
        repo.initialize();
    }

    @After
    public void tearDown() {
        blobs.clear();
        repo.close();
    }

    @Test
    public void listFolders() throws IOException {
        // Find projects in the rules folder
        List<FileData> folders = repo.listFolders("rules/");
        assertNotNull(folders);
        assertEquals(2, folders.size());

        assertContains(folders, "rules/project1");
        assertContains(folders, "rules/project2");

        // Finding folders inside the projects
        List<FileData> subFolders = repo.listFolders("rules/project2/");
        assertNotNull(subFolders);
        assertEquals(2, subFolders.size());
        assertContains(subFolders, "rules/project2/folder1");
        assertContains(subFolders, "rules/project2/folder2");
    }

    @Test
    public void listFiles() throws IOException {
        List<FileData> files1 = repo.listFiles("rules/project1/", "version11");
        assertNotNull(files1);
        assertEquals(2, files1.size());
        assertContains(files1, "rules/project1/file11");
        assertContains(files1, "rules/project1/file12");

        List<FileData> files2 = repo.listFiles("rules/project2/", "version21");
        assertNotNull(files2);
        assertEquals(2, files2.size());
        assertContains(files2, "rules/project2/folder1/file23");
        assertContains(files2, "rules/project2/folder2/file24");
    }

    @Test
    public void saveFolder() throws IOException {
        List<FileItem> changes = Arrays.asList(
            new FileItem("rules/project1/new-path/file14", IOUtils.toInputStream("Added")),
            new FileItem("rules/project1/file11", IOUtils.toInputStream("Modified")));

        FileData folderData = createFileData("rules/project1");

        FileData savedData = repo.save(folderData, changes, ChangesetType.DIFF);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file14");
        assertContains(files, "rules/project1/file11");
        assertContains(files, "rules/project1/file12");
        assertEquals(3, files.size());
    }

    @Test
    public void list() throws IOException {
        assertEquals(4, repo.list("").size());
        assertEquals(2, repo.list("rules/project1/").size());
        assertEquals(0, repo.list("rules/project1/folder1").size());
        assertEquals(2, repo.list("rules/project2/").size());
        assertEquals(1, repo.list("rules/project2/folder1").size());
    }

    @Test
    public void check() throws IOException {
        assertNull(repo.check("rules/project1/absent-file"));

        FileData file1 = repo.check("rules/project1/file11");
        assertNotNull(file1);
        assertEquals(createFileContent(file1.getName()).length(), file1.getSize());

        FileData file2 = repo.check("rules/project2/folder2/file24");
        assertNotNull(file2);
        assertEquals(createFileContent(file2.getName()).length(), file2.getSize());
    }

    @Test
    public void read() throws IOException {
        assertNull(repo.read("rules/project1/absent-file"));

        assertEquals(createFileContent("rules/project1/file11"),
            IOUtils.toStringAndClose(repo.read("rules/project1/file11").getStream()));
        assertEquals(createFileContent("rules/project2/folder1/file23"),
            IOUtils.toStringAndClose(repo.read("rules/project2/folder1/file23").getStream()));
    }

    @Test
    public void delete() throws IOException {
        String projectPath = "rules/project1";
        // Archive the project
        FileData projectData = new FileData();
        projectData.setName(projectPath);
        projectData.setComment("Delete project1");
        projectData.setAuthor(new UserInfo("john_smith"));
        assertTrue("'project1' has not been deleted", repo.delete(projectData));

        FileData deletedProject = repo.check(projectPath);
        assertTrue("'project1' is not deleted", deletedProject.isDeleted());

        // Restore the project
        FileData toDelete = new FileData();
        toDelete.setAuthor(new UserInfo("john_smith"));
        toDelete.setName(projectPath);
        toDelete.setVersion(deletedProject.getVersion());
        toDelete.setComment("Delete project1.");
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertFalse("'project1' is not restored", deletedProject.isDeleted());
        assertEquals("Delete project1.", deletedProject.getComment());

        // Count actual changes in history
        assertEquals("Actual project changes must be 3.", 3, repo.listHistory(projectPath).size());

        // Erase the project
        toDelete.setName(projectPath);
        toDelete.setVersion(null);
        toDelete.setComment("Erase project1");
        assertTrue(repo.deleteHistory(toDelete));
        deletedProject = repo.check(projectPath);
        assertNull("'project1' is not erased", deletedProject);
    }

    @Test
    public void listHistory() throws IOException {
        List<FileData> project2History = repo.listHistory("rules/project2");
        assertEquals(1, project2History.size());
        assertEquals("version21", project2History.get(0).getVersion());

        final String newVersion = addFileToProject2AndSave().getVersion();

        project2History = repo.listHistory("rules/project2");
        assertEquals(2, project2History.size());
        assertEquals("version21", project2History.get(0).getVersion());
        assertEquals(newVersion, project2History.get(1).getVersion());
    }

    @Test
    public void checkHistory() throws IOException {
        assertEquals("version11", repo.checkHistory("rules/project1/file11", "version11").getVersion());
        assertNull(repo.checkHistory("rules/project1/file11", "absent"));

        assertEquals("version11", repo.checkHistory("rules/project1", "version11").getVersion());

        final String newVersion = addFileToProject2AndSave().getVersion();
        assertEquals("version21", repo.checkHistory("rules/project2", "version21").getVersion());
        assertEquals(newVersion, repo.checkHistory("rules/project2", newVersion).getVersion());

        assertNull(repo.checkHistory("rules/project2", "absent"));
    }

    @Test
    public void readHistory() throws IOException {
        addFileToProject2AndSave();

        final String fileInProject2 = "rules/project2/folder1/file23";
        final String content = createFileContent(fileInProject2);
        assertEquals(content, IOUtils.toStringAndClose(repo.readHistory(fileInProject2, "version21").getStream()));

        assertNull(repo.readHistory(fileInProject2, "absent"));
    }

    @Test
    public void copyHistory() throws IOException {
        FileData destProject = new FileData();
        destProject.setName("rules/project-copy");
        destProject.setComment("Copy of project1");
        destProject.setAuthor(new UserInfo("john_smith"));
        FileData projectCopy = repo.copyHistory("rules/project1", destProject, "version11");
        assertNotNull(projectCopy);
        assertEquals("rules/project-copy", projectCopy.getName());
        assertEquals("john_smith", projectCopy.getAuthor().getName());
        assertEquals("Copy of project1", projectCopy.getComment());
        assertEquals(FileData.UNDEFINED_SIZE, projectCopy.getSize());
        List<FileData> projectCopyFiles = repo.list("rules/project-copy/");
        assertEquals(2, projectCopyFiles.size());
        assertContains(projectCopyFiles, "rules/project-copy/file11");
        assertContains(projectCopyFiles, "rules/project-copy/file12");
    }

    private BlobContainerClient mockContainerClient() throws IOException {
        BlobContainerClient client = mock(BlobContainerClient.class);
        when(client.listBlobs(any(), any())).thenAnswer(invocation -> mockListBlobs(invocation.getArgument(0)));
        when(client.getBlobVersionClient(any(), any()))
            .thenAnswer(invocation -> mockGetBlobVersionClient(invocation.getArgument(0), invocation.getArgument(1)));
        when(client.getBlobClient(any()))
            .thenAnswer(invocation -> mockGetBlobVersionClient(invocation.getArgument(0), null));

        addBlobsForProject("rules/project1", "version11", "rules/project1/file11", "rules/project1/file12");
        addBlobsForProject("rules/project2",
            "version21",
            "rules/project2/folder1/file23",
            "rules/project2/folder2/file24");

        return client;
    }

    @SuppressWarnings("unchecked")
    private PagedIterable<BlobItem> mockPagedIterable(Iterable<BlobItem> iterable) {
        PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(iterable.iterator());
        return pagedIterable;
    }

    private PagedIterable<BlobItem> mockListBlobs(ListBlobsOptions options) {
        if (options.getDetails().getRetrieveVersions()) {
            List<BlobEmulation> versions = blobs.get(options.getPrefix());
            final List<BlobItem> list = new ArrayList<>(
                versions == null ? Collections.emptyList()
                                 : versions.stream().map(BlobEmulation::getBlobItem).collect(Collectors.toList()));
            // To conform behavior of Azure Blob Storage
            Collections.reverse(list);
            return mockPagedIterable(list);
        }
        return mockPagedIterable(blobs.entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith(options.getPrefix()))
            .map(entry -> entry.getValue().get(0).getBlobItem())
            .collect(Collectors.toList()));
    }

    private BlobClient mockGetBlobVersionClient(String blobName, String versionId) throws IOException {
        final List<BlobEmulation> versions = blobs.get(blobName);
        if (versions != null) {
            for (BlobEmulation blob : versions) {
                if (versionId == null || versionId.equals(blob.getBlobItem().getVersionId())) {
                    return mockBlobClient(blob, blobName, versionId);
                }
            }
        }

        return mockBlobClient(null, blobName, versionId);
    }

    private BlobClient mockBlobClient(BlobEmulation blob, String blobName, String versionId) throws IOException {
        BlobClient client = mock(BlobClient.class);
        when(client.exists()).thenReturn(blob != null);
        if (blob != null) {
            blobName = blob.getBlobItem().getName();
            versionId = blob.getBlobItem().getVersionId();

            BlobInputStream stream = mockBlobInputStream(new ByteArrayInputStream(blob.getContent()));
            when(client.openInputStream()).thenReturn(stream);
        }
        when(client.getBlobName()).thenReturn(blobName);
        when(client.getVersionId()).thenReturn(versionId);
        when(client.uploadWithResponse(any(), any(), any()))
            .thenAnswer(invocation -> mockUploadWithResponse(client, invocation.getArgument(0)));
        BlobProperties properties = mockBlobProperties(versionId, blob);
        when(client.getProperties()).thenReturn(properties);

        doAnswer(invocation -> mockDelete(Objects.requireNonNull(blob))).when(client).delete();

        when(client.downloadContent()).thenAnswer(invocation -> BinaryData.fromBytes(Objects.requireNonNull(blob)
            .getContent()));
        return client;
    }

    private Object mockDelete(BlobEmulation blob) {
        blobs.remove(blob.getBlobItem().getName());

        return null;
    }

    private BlobProperties mockBlobProperties(String versionId, BlobEmulation blob) {
        BlobProperties properties = mock(BlobProperties.class);
        when(properties.getVersionId()).thenReturn(versionId);
        when(properties.getCreationTime()).thenReturn(new Date().toInstant().atOffset(ZoneOffset.UTC));
        if (blob != null) {
            when(properties.getBlobSize()).thenReturn((long) blob.getContent().length);
        }
        return properties;
    }

    private BlobInputStream mockBlobInputStream(ByteArrayInputStream byteStream) throws IOException {
        BlobInputStream stream = mock(BlobInputStream.class);
        when(stream.read()).thenAnswer(delegatesTo(byteStream));
        when(stream.read(any())).thenAnswer(delegatesTo(byteStream));
        when(stream.read(any(), anyInt(), anyInt())).thenAnswer(delegatesTo(byteStream));
        return stream;
    }

    private Response<BlockBlobItem> mockUploadWithResponse(BlobClient client, BlobParallelUploadOptions options) {
        String versionId = UUID.randomUUID().toString();

        final List<ByteBuffer> block = options.getDataFlux().collectList().block();
        final ByteBuffer byteBuffer = Objects.requireNonNull(block).get(0);
        byte[] content = new byte[byteBuffer.remaining()];
        byteBuffer.get(content);
        addBlob(client.getBlobName(), versionId, content);

        return mockResponse(versionId);
    }

    @SuppressWarnings("unchecked")
    private Response<BlockBlobItem> mockResponse(String versionId) {
        Response<BlockBlobItem> response = mock(Response.class);
        final BlockBlobItem blockBlobItem = mockBlockBlobItem(versionId);
        when(response.getValue()).thenReturn(blockBlobItem);
        return response;
    }

    private BlockBlobItem mockBlockBlobItem(String versionId) {
        BlockBlobItem blockBlobItem = mock(BlockBlobItem.class);
        when(blockBlobItem.getVersionId()).thenReturn(versionId);
        return blockBlobItem;
    }

    private void addBlobsForProject(String projectName,
            String versionId,
            String... fileNamesInProject) throws IOException {
        AzureCommit commit = new AzureCommit();
        final ArrayList<FileInfo> files = new ArrayList<>();
        for (String fileName : fileNamesInProject) {
            String fileContent = createFileContent(fileName);
            final String revision = UUID.randomUUID().toString();

            addBlob(AzureBlobRepository.CONTENT_PREFIX + fileName,
                revision,
                fileContent.getBytes(StandardCharsets.UTF_8));

            FileInfo fileInfo = new FileInfo();
            fileInfo.setPath(fileName);
            fileInfo.setRevision(revision);
            files.add(fileInfo);
        }
        commit.setFiles(files);
        commit.setComment("Edit " + projectName);
        commit.setAuthor("john_smith");

        String commitFile = AzureBlobRepository.VERSIONS_PREFIX + projectName + "/" + AzureBlobRepository.VERSION_FILE;
        addBlob(commitFile, versionId, toBytes(commit));
    }

    private String createFileContent(String fileName) {
        return "Hello " + fileName;
    }

    private void addBlob(String name, String versionId, byte[] content) {
        final BlobItem blobItem = new BlobItem();
        blobItem.setName(name);
        blobItem.setVersionId(versionId);
        final List<BlobEmulation> versions = blobs.computeIfAbsent(name, k -> new ArrayList<>());
        // Newest version will be first.
        versions.add(0, new BlobEmulation(blobItem, content));
    }

    private byte[] toBytes(AzureCommit commit) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            AzureBlobRepository.createYamlForCommit().dump(commit, out);
        }
        return outputStream.toByteArray();
    }

    private static void assertContains(List<FileData> files, String fileName) {
        boolean contains = false;
        for (FileData file : files) {
            if (fileName.equals(file.getName())) {
                contains = true;
                break;
            }
        }

        assertTrue("Files list does not contain the file '" + fileName + "'", contains);
    }

    private FileData createFileData(String project) {
        FileData folderData = new FileData();
        folderData.setName(project);
        folderData.setAuthor(new UserInfo("john_smith"));
        folderData.setComment("Bulk change");
        return folderData;
    }

    private FileData addFileToProject2AndSave() throws IOException {
        List<FileItem> changes = List
            .of(new FileItem("rules/project2/new-path/new-file", IOUtils.toInputStream("Added")));
        FileData folderData = createFileData("rules/project2");

        return repo.save(folderData, changes, ChangesetType.DIFF);
    }
}