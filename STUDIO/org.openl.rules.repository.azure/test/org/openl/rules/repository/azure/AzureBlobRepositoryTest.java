package org.openl.rules.repository.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
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

        assertEquals("rules/project1", folders.get(0).getName());
        assertEquals("rules/project2", folders.get(1).getName());

        // Finding folders inside the projects
        List<FileData> subFolders = repo.listFolders("rules/project2/");
        assertNotNull(subFolders);
        assertEquals(2, subFolders.size());
        assertEquals("rules/project2/folder1", subFolders.get(0).getName());
        assertEquals("rules/project2/folder2", subFolders.get(1).getName());
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

        FileData folderData = new FileData();
        folderData.setName("rules/project1");
        folderData.setAuthor(new UserInfo("john_smith", "jsmith@email", "John Smith"));
        folderData.setComment("Bulk change");

        FileData savedData = repo.save(folderData, changes, ChangesetType.DIFF);
        assertNotNull(savedData);
        List<FileData> files = repo.list("rules/project1/");
        assertContains(files, "rules/project1/new-path/file14");
        assertContains(files, "rules/project1/file11");
        assertContains(files, "rules/project1/file12");
        assertEquals(3, files.size());
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
        return client;
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
        if (block == null) {
            throw new IllegalStateException();
        }
        final ByteBuffer byteBuffer = block.get(0);
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
            String fileContent = "Hello " + fileName;
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

        String commitFile = AzureBlobRepository.VERSIONS_PREFIX + projectName + "/" + AzureBlobRepository.VERSION_FILE;
        addBlob(commitFile, versionId, toBytes(commit));
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
}