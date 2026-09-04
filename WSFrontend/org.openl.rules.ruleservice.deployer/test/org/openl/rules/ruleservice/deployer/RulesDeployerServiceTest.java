package org.openl.rules.ruleservice.deployer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.IOUtils;

@Disabled
class RulesDeployerServiceTest {

    private static final String MULTIPLE_DEPLOYMENT = "multiple-deployment.zip";
    private static final String SINGLE_DEPLOYMENT = "single-deployment.zip";
    private static final String NO_NAME_DEPLOYMENT = "no-name-deployment.zip";
    private static final String DEPLOY_PATH = "deploy/";

    private Repository mockedDeployRepo;
    private RulesDeployerService deployer;
    private ArgumentCaptor<FileData> fileDataCaptor;
    private ArgumentCaptor<InputStream> streamCaptor;
    private ArgumentCaptor<FileChangesFromZip> fileChangesFromZipCaptor;

    @BeforeEach
    void setUp() throws IOException {
        fileDataCaptor = forClass(FileData.class);
        streamCaptor = forClass(InputStream.class);
        fileChangesFromZipCaptor = forClass(FileChangesFromZip.class);
    }

    private <T extends Repository> void init(Class<T> repo, boolean local) throws IOException {
        mockedDeployRepo = mock(repo);
        when(mockedDeployRepo.supports()).thenReturn(new FeaturesBuilder(mockedDeployRepo).setLocal(local).build());
        when(mockedDeployRepo.list(anyString())).thenReturn(List.of());
        deployer = new RulesDeployerService(mockedDeployRepo, DEPLOY_PATH);
    }

    @Test
    void test_deploy_singleDeployment() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    void test_deploy_singleDeployment_whenFoldersSupports() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
        verify(mockedDeployRepo, times(1))
                .save(fileDataCaptor.capture(), fileChangesFromZipCaptor.capture(), eq(ChangesetType.FULL));
        assertNotNull(fileChangesFromZipCaptor.getValue());
        assertNotNull(fileDataCaptor.getValue());
    }

    @Test
    void test_deploy_singleDeployment_with_custom_name() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy("customName", is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    void test_deploy_without_description() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(NO_NAME_DEPLOYMENT)) {
            deployer.deploy("customName", is, true);
        }
        verify(mockedDeployRepo, times(1)).save(fileDataCaptor.capture(), any(InputStream.class));
        final var actualFileData = fileDataCaptor.getValue();
        assertEquals("deploy/customName/customName", actualFileData.getName());
    }

    @Test
    void test_multideploy_without_name() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream("noname-multiple-deployment.zip")) {
            deployer.deploy("customName-deployment", is, true);
        }
        List<FileItem> actualFileItems = catchDeployFileItems();
        final var baseDeploymentPath = DEPLOY_PATH + "customName-deployment/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);
    }

    @Test
    void test_deploy_singleDeployment_whenNotOverridable() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    void test_deploy_singleDeployment_whenNotOverridableAndDeployedAlready() throws Exception {
        init(Repository.class, false);
        when(mockedDeployRepo.list(DEPLOY_PATH + "project2/")).thenReturn(List.of(new FileData()));
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void test_deploy_singleDeployment_whenOverridableAndDeployedAlready() throws Exception {
        init(Repository.class, false);
        when(mockedDeployRepo.list(DEPLOY_PATH + "project2/")).thenReturn(List.of(new FileData()));
        try (var is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    private void assertSingleDeployment(String expectedName) throws IOException {
        verify(mockedDeployRepo, times(1)).save(fileDataCaptor.capture(), streamCaptor.capture());
        final var actualFileData = fileDataCaptor.getValue();
        assertNotNull(actualFileData);
        assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor().getUsername());
        assertTrue(actualFileData.getSize() > 0, "Content size must be greater thar 0");
        assertEquals(expectedName, actualFileData.getName());
    }

    @Test
    void test_deploy_multipleDeployment() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream(MULTIPLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        List<FileItem> actualFileItems = catchDeployFileItems();
        final var baseDeploymentPath = DEPLOY_PATH + "yaml_project/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);
    }

    @Test
    void testRead() throws IOException {
        init(Repository.class, false);
        final var baseDeploymentPath = DEPLOY_PATH + "yaml_project/";
        when(mockedDeployRepo.read(baseDeploymentPath + "project1"))
                .thenReturn(createFileItem(baseDeploymentPath + "project1", "single-deployment.zip"));
        when(mockedDeployRepo.read(baseDeploymentPath + "project2"))
                .thenReturn(createFileItem(baseDeploymentPath + "project2", "no-name-deployment.zip"));
        var output = new ByteArrayOutputStream();
        deployer.read("yaml_project", toSet("yaml_project/project1", "yaml_project/project2"), output);
        Map<String, byte[]> entries = unzip(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(4, entries.size());
        assertNotNull(entries.get("project2/no-name-deployment.xlsx"));
        assertNotNull(entries.get("project1/rules.xml"));
        assertNotNull(entries.get("project1/rules/Project2-Main.xlsx"));
        assertNotNull(entries.get("project1/rules-deploy.xml"));
    }

    @Test
    void testRead2() throws IOException {
        init(Repository.class, false);
        when(mockedDeployRepo.read(DEPLOY_PATH + "project2/project2"))
                .thenReturn(createFileItem(DEPLOY_PATH + "project2/project2", "single-deployment.zip"));
        var output = new ByteArrayOutputStream();
        deployer.read("project2", toSet("project2/project2"), output);
        final var actualBytes = output.toByteArray();
        final byte[] expectedBytes = toByteArray(getResourceAsStream("single-deployment.zip"));
        assertEquals(expectedBytes.length, actualBytes.length);
        for (var i = 0; i < expectedBytes.length; i++) {
            assertEquals(expectedBytes[i], actualBytes[i]);
        }
    }

    private FileItem createFileItem(String projectName, String pathToArchive) {
        var data = new FileData();
        data.setName(projectName);
        return new FileItem(data, getResourceAsStream(pathToArchive));
    }

    @Test
    void test_EPBDS_10894() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy(is, true);
        }
        assertEPBDS_10894();
    }

    @Test
    void test_EPBDS_10894_CustomName_mustNotApplied() throws Exception {
        init(Repository.class, false);
        try (var is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy("EPBDS-10894.zip", is, true);
        }
        assertEPBDS_10894();
    }

    @Test
    void testMultiDeploymentFolderSupport_CustomName_mustNotApplied() throws Exception {
        init(Repository.class, true);
        try (var is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy("EPBDS-10894.zip", is, true);
        }
        var folderData = catchDeployFolders();
        assertEquals("EPBDS-10894_yaml_project", folderData.getName());
        assertEquals(13251, folderData.getSize());
    }

    @Test
    void testMultiDeploymentFolderSupport_NoDeploymentName() throws Exception {
        init(Repository.class, true);
        try (var is = getResourceAsStream("noname-multiple-deployment.zip")) {
            deployer.deploy("customName-deployment", is, true);
        }
        var folderData = catchDeployFolders();
        assertEquals("customName-deployment", folderData.getName());
        assertEquals(13770, folderData.getSize());
    }

    @Test
    void testWrongFile() throws Exception {
        init(Repository.class, true);
        try {
            deployer.deploy("customName-deployment", new ByteArrayInputStream("foo".getBytes()), true);
            fail("Everything went different before...");
        } catch (Exception e) {
            assertEquals("Provided file is not an archive!", e.getMessage());
        }
        try {
            var baos = new ByteArrayOutputStream();
            IOUtils.closeQuietly(new ZipOutputStream(baos)); // make it empty
            deployer.deploy("customName-deployment", new ByteArrayInputStream(baos.toByteArray()), true);
            fail("Everything went different before...");
        } catch (Exception e) {
            assertEquals("Cannot create a project from the given file. Zip file is empty.", e.getMessage());
        }
    }

    private void assertEPBDS_10894() throws IOException {
        List<FileItem> actualFileItems = catchDeployFileItems();
        final var baseDeploymentPath = DEPLOY_PATH + "EPBDS-10894_yaml_project/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);
    }

    private List<FileItem> catchDeployFileItems() throws IOException {
        Class<List<FileItem>> listClass = (Class) List.class;
        var captor = forClass(listClass);

        verify(mockedDeployRepo, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private FileData catchDeployFolders() throws IOException {
        Class<FileData> fileDataClass = FileData.class;
        Class<List<FileItem>> listClass = (Class) List.class;
        var captor1 = forClass(fileDataClass);
        var captor2 = forClass(listClass);

        verify((Repository) mockedDeployRepo, times(1)).save(captor1.capture(), captor2.capture(), eq(ChangesetType.FULL));
        return captor1.getValue();
    }

    private void assertMultipleDeployment(Set<String> expectedNames, List<FileItem> actualFileDatas) {
        assertFalse(actualFileDatas.isEmpty());
        var namesToVerify = new HashSet<String>(expectedNames);
        var unexpectedNames = new HashSet<String>();
        for (FileItem actualFileItem : actualFileDatas) {
            final var actualFileData = actualFileItem.getData();
            assertNotNull(actualFileData);
            assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor().getUsername());
            assertTrue(actualFileData.getSize() > 0, "Content size must be greater than 0");
            if (namesToVerify.contains(actualFileData.getName())) {
                namesToVerify.remove(actualFileData.getName());
            } else {
                unexpectedNames.add(actualFileData.getName());
            }
        }
        if (!unexpectedNames.isEmpty()) {
            fail("Unexpected deployment names: %s".formatted(String.join(", ", unexpectedNames)));
        }
        if (!namesToVerify.isEmpty()) {
            fail("Missed expected deployment names: %s".formatted(String.join(", ", namesToVerify)));
        }
    }

    private InputStream getResourceAsStream(String name) {
        return RulesDeployerServiceTest.class.getClassLoader().getResourceAsStream(name);
    }

    private Set<String> toSet(String... args) {
        return Stream.of(args).collect(Collectors.toSet());
    }

    static Map<String, byte[]> unzip(InputStream in) throws IOException {
        var entries = new HashMap<String, byte[]>();
        try (var zipStream = new ZipInputStream(in)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                var name = zipEntry.getName();
                var outputStream = new ByteArrayOutputStream();
                IOUtils.copyAndClose(new ZippedFileInputStream(zipStream), outputStream);
                entries.put(name, outputStream.toByteArray());
            }
        }
        return entries;
    }

    private static byte[] toByteArray(InputStream source) throws IOException {
        var target = new ByteArrayOutputStream();
        IOUtils.copyAndClose(source, target);
        return target.toByteArray();
    }
}
