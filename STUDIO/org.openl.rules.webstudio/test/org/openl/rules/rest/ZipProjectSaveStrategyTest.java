package org.openl.rules.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.dtr.impl.MappedRepository;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.IOUtils;
import org.openl.util.ZipUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZipProjectSaveStrategyTest {

    private static final String BASE_RULES_LOCATION = "DESIGN/";

    private ZipProjectSaveStrategy saveStrategy;
    private DesignTimeRepository designTimeRepositoryMock;
    private UserManagementService userManagementService;
    private ArgumentCaptor<FileData> fileDataCaptor;
    private XmlProjectDescriptorSerializer projectDescriptorSerializer;

    @Before
    public void setUp() {
        this.projectDescriptorSerializer = new XmlProjectDescriptorSerializer();
        this.fileDataCaptor = ArgumentCaptor.forClass(FileData.class);

        this.designTimeRepositoryMock = mock(DesignTimeRepository.class);
        this.userManagementService = mock(UserManagementService.class);
        when(designTimeRepositoryMock.getRulesLocation()).thenReturn(BASE_RULES_LOCATION);
        User user = new User();
        user.setDisplayName("John Smith");
        user.setEmail("jsmith@email");
        when(userManagementService.getUser(anyString())).thenReturn(user);

        ZipCharsetDetector zipCharsetDetectorMock = mock(ZipCharsetDetector.class);
        when(zipCharsetDetectorMock.detectCharset(any())).thenReturn(StandardCharsets.UTF_8);

        PathFilter zipFilterMock = mock(PathFilter.class);
        when(zipFilterMock.accept(anyString())).thenReturn(Boolean.TRUE);

        saveStrategy = new ZipProjectSaveStrategy(designTimeRepositoryMock,
            zipFilterMock,
            zipCharsetDetectorMock,
            userManagementService);
    }

    @Test
    public void testSaveMappedRepo() throws IOException {
        mockDesignRepository(MappedRepository.class, "design1", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design1",
            "jsmith",
            "Project 1",
            "foo/Project 1",
            "Bar",
            false);
        FolderRepository repo = (FolderRepository) designTimeRepositoryMock.getRepository(model.getRepoName());
        Map<String, FileItem> actualFileItems = captureFileItems(repo);

        Path expected = Paths.get("test-resources/upload/zip/project.zip");
        saveStrategy.save(model, expected);
        verify(repo, times(1)).save(fileDataCaptor.capture(), any(), eq(ChangesetType.FULL));
        assertSame(expected, BASE_RULES_LOCATION + "Project 1/", actualFileItems);

        FileData actualData = fileDataCaptor.getValue();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualData.getName());
        assertEquals("Bar", actualData.getComment());
        assertEquals("jsmith@email", actualData.getAuthor().getEmail());
        assertEquals("John Smith", actualData.getAuthor().getDisplayName());
        assertEquals(1, actualData.getAdditionalData().size());
        FileMappingData actualAddData = (FileMappingData) actualData.getAdditionalData().values().iterator().next();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualAddData.getExternalPath());
        assertEquals("foo/Project 1", actualAddData.getInternalPath());
    }

    @Test
    public void testSaveMappedRepo2() throws IOException {
        mockDesignRepository(MappedRepository.class, "design1", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design1",
            "jsmith",
            "Project 1",
            null,
            "Bar",
            false);
        FolderRepository repo = (FolderRepository) designTimeRepositoryMock.getRepository(model.getRepoName());
        Map<String, FileItem> actualFileItems = captureFileItems(repo);

        Path expected = Paths.get("test-resources/upload/zip/project.zip");
        saveStrategy.save(model, expected);
        verify(repo, times(1)).save(fileDataCaptor.capture(), any(), eq(ChangesetType.FULL));
        assertSame(expected, BASE_RULES_LOCATION + "Project 1/", actualFileItems);

        FileData actualData = fileDataCaptor.getValue();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualData.getName());
        assertEquals("Bar", actualData.getComment());
        assertEquals("jsmith@email", actualData.getAuthor().getEmail());
        assertEquals("John Smith", actualData.getAuthor().getDisplayName());
        assertEquals(1, actualData.getAdditionalData().size());
        FileMappingData actualAddData = (FileMappingData) actualData.getAdditionalData().values().iterator().next();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualAddData.getExternalPath());
        assertEquals("Project 1", actualAddData.getInternalPath());
    }

    @Test
    public void testSaveNotFolderRepo() throws IOException {
        mockDesignRepository(Repository.class, "design2", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design2",
            "jsmith",
            "Project 1",
            null,
            null,
            false);
        Repository repo = designTimeRepositoryMock.getRepository(model.getRepoName());
        AtomicReference<InputStream> actualStream = captureStream(repo);

        Path expected = Paths.get("test-resources/upload/zip/project.zip");
        saveStrategy.save(model, expected);
        verify(repo, times(1)).save(fileDataCaptor.capture(), any());
        assertSame(expected, actualStream.get());

        FileData actualData = fileDataCaptor.getValue();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualData.getName());
        assertEquals("", actualData.getComment());
        assertEquals("jsmith@email", actualData.getAuthor().getEmail());
        assertEquals("John Smith", actualData.getAuthor().getDisplayName());
        assertEquals(0, actualData.getAdditionalData().size());
    }

    @Test
    public void testSaveMappedRepoCustomPath() throws IOException {
        mockDesignRepository(MappedRepository.class, "design1", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design1",
            "jsmith",
            "Project 1",
            "custom-name",
            "Bar",
            false);
        FolderRepository repo = (FolderRepository) designTimeRepositoryMock.getRepository(model.getRepoName());
        Map<String, FileItem> actualFileItems = captureFileItems(repo);

        Path expected = Paths.get("test-resources/upload/zip/project.zip");
        saveStrategy.save(model, expected);
        verify(repo, times(1)).save(fileDataCaptor.capture(), any(), eq(ChangesetType.FULL));
        final String expectedRootFolder = BASE_RULES_LOCATION + "Project 1/";
        assertSame(expected, expectedRootFolder, actualFileItems);

        FileData actualData = fileDataCaptor.getValue();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualData.getName());
        assertEquals("Bar", actualData.getComment());
        assertEquals("jsmith@email", actualData.getAuthor().getEmail());
        assertEquals("John Smith", actualData.getAuthor().getDisplayName());
        assertEquals(1, actualData.getAdditionalData().size());
        FileMappingData actualAddData = (FileMappingData) actualData.getAdditionalData().values().iterator().next();
        assertEquals(BASE_RULES_LOCATION + "Project 1", actualAddData.getExternalPath());
        assertEquals("custom-name", actualAddData.getInternalPath());

        FileItem descriptor = actualFileItems
            .get(expectedRootFolder + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        ((ByteArrayInputStream) descriptor.getStream()).reset();
        assertProjectDescriptor(expectedRootFolder, "Project 1", descriptor);
    }

    @Test
    public void testSaveMappedRepoCustomPathExtraProjectDescriptor() throws IOException {
        mockDesignRepository(MappedRepository.class, "design1", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design1",
            "jsmith",
            "Project 2",
            "custom-name",
            "Bar",
            false);
        FolderRepository repo = (FolderRepository) designTimeRepositoryMock.getRepository(model.getRepoName());
        Map<String, FileItem> actualFileItems = captureFileItems(repo);

        Path expected = Paths.get("test-resources/upload/zip/excel-only-project.zip");
        saveStrategy.save(model, expected);
        verify(repo, times(1)).save(fileDataCaptor.capture(), any(), eq(ChangesetType.FULL));

        final String expectedRootFolder = BASE_RULES_LOCATION + "Project 2/";
        FileItem descriptor = actualFileItems
            .remove(expectedRootFolder + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        assertProjectDescriptor(expectedRootFolder, "Project 2", descriptor);

        assertSame(expected, expectedRootFolder, actualFileItems);

        FileData actualData = fileDataCaptor.getValue();
        assertEquals(BASE_RULES_LOCATION + "Project 2", actualData.getName());
        assertEquals("Bar", actualData.getComment());
        assertEquals("jsmith@email", actualData.getAuthor().getEmail());
        assertEquals("John Smith", actualData.getAuthor().getDisplayName());
        assertEquals(1, actualData.getAdditionalData().size());
        FileMappingData actualAddData = (FileMappingData) actualData.getAdditionalData().values().iterator().next();
        assertEquals(BASE_RULES_LOCATION + "Project 2", actualAddData.getExternalPath());
        assertEquals("custom-name", actualAddData.getInternalPath());
    }

    private void assertProjectDescriptor(String expectedRootFolder, String expectedName, FileItem descriptor) {
        assertNotNull(descriptor);
        assertEquals(expectedRootFolder + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME,
            descriptor.getData().getName());
        assertEquals(expectedName, projectDescriptorSerializer.deserialize(descriptor.getStream()).getName());
    }

    private static void assertSame(Path expectedArchive, InputStream actualStream) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(expectedArchive),
            Collections.singletonMap("encoding", StandardCharsets.UTF_8.displayName()))) {
            Path root = fs.getPath("/");
            try (ZipInputStream actualZipStream = new ZipInputStream(actualStream)) {
                ZipEntry ze;
                while ((ze = actualZipStream.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        Path expected = root.resolve(ze.getName());
                        try (InputStream expectedStream = Files.newInputStream(expected)) {
                            if (!ze.getName().equals("rules.xml")) {
                                assertTrue(
                                    org.apache.commons.io.IOUtils.contentEquals(expectedStream, actualZipStream));
                            } else {
                                // rules xml must be modified
                                assertFalse(
                                    org.apache.commons.io.IOUtils.contentEquals(expectedStream, actualZipStream));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void assertSame(Path expectedArchive,
            String expectedPrefix,
            Map<String, FileItem> actualFileItems) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(expectedArchive),
            Collections.singletonMap("encoding", StandardCharsets.UTF_8.displayName()))) {
            Path root = fs.getPath("/");
            actualFileItems.forEach((actualName, actualItem) -> {
                assertTrue(actualName.startsWith(expectedPrefix));
                String actualFileName = actualName.substring(expectedPrefix.length());
                Path expected = root.resolve(actualFileName);
                assertTrue(Files.exists(expected));
                try (InputStream expectedStream = Files.newInputStream(expected);
                        InputStream actualStream = actualItem.getStream()) {
                    if (!actualFileName.equals("rules.xml")) {
                        assertTrue(org.apache.commons.io.IOUtils.contentEquals(expectedStream, actualStream));
                    } else {
                        // rules xml must be modified
                        assertFalse(org.apache.commons.io.IOUtils.contentEquals(expectedStream, actualStream));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private Map<String, FileItem> captureFileItems(FolderRepository repo) throws IOException {
        Map<String, FileItem> actualFileItems = new HashMap<>();
        when(repo.save(any(FileData.class), any(), eq(ChangesetType.FULL))).thenAnswer(a -> {
            // noinspection unchecked
            for (FileItem fileItem : (Iterable<FileItem>) a.getArguments()[1]) {
                if (actualFileItems.containsKey(fileItem.getData().getName())) {
                    throw new RuntimeException("Unexpected entry!");
                }
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                IOUtils.copyAndClose(fileItem.getStream(), os);
                actualFileItems.put(fileItem.getData().getName(),
                    new FileItem(fileItem.getData(), new ByteArrayInputStream(os.toByteArray())));
            }
            return null;
        });
        return actualFileItems;
    }

    private AtomicReference<InputStream> captureStream(Repository repo) throws IOException {
        AtomicReference<InputStream> holder = new AtomicReference<>();
        when(repo.save(any(FileData.class), any())).thenAnswer(a -> {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IOUtils.copyAndClose((InputStream) a.getArguments()[1], os);
            holder.set(new ByteArrayInputStream(os.toByteArray()));
            return null;
        });
        return holder;
    }

    private <T extends Repository> T mockDesignRepository(Class<T> tClass,
            String repoName,
            Consumer<FeaturesBuilder> featureConfig) throws IOException {
        T mockedRepo = mock(tClass);
        when(designTimeRepositoryMock.getRepository(repoName)).thenReturn(mockedRepo);

        when(mockedRepo.check(anyString())).thenReturn(null);

        FeaturesBuilder featuresBuilder = new FeaturesBuilder(mockedRepo);
        if (MappedRepository.class.isAssignableFrom(tClass)) {
            when(((MappedRepository) mockedRepo).getDelegate()).thenReturn(((MappedRepository) mockedRepo));
            featuresBuilder.setMappedFolders(true);
        }
        featureConfig.accept(featuresBuilder);
        when(mockedRepo.supports()).thenReturn(featuresBuilder.build());

        return mockedRepo;
    }
}
