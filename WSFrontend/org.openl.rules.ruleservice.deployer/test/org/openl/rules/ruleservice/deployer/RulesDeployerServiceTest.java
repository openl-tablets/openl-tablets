package org.openl.rules.ruleservice.deployer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromZip;

public class RulesDeployerServiceTest {

    private static final String MULTIPLE_DEPLOYMENT = "multiple-deployment.zip";
    private static final String SINGLE_DEPLOYMENT = "single-deployment.zip";
    private static final String NO_NAME_DEPLOYMENT = "no-name-deployment.zip";
    private static final String DEPLOY_PATH = "deploy/";

    private Repository mockedDeployRepo;
    private RulesDeployerService deployer;
    private ArgumentCaptor<FileData> fileDataCaptor;
    private ArgumentCaptor<FileChangesFromZip> fileChangesFromZipCaptor;

    @Before
    public void setUp() throws IOException {
        fileDataCaptor = ArgumentCaptor.forClass(FileData.class);
        fileChangesFromZipCaptor = ArgumentCaptor.forClass(FileChangesFromZip.class);
    }

    private <T extends Repository> void init(Class<T> repo, boolean local) throws IOException {
        mockedDeployRepo = mock(repo);
        when(mockedDeployRepo.supports()).thenReturn(new FeaturesBuilder(mockedDeployRepo).setLocal(local).build());
        when(mockedDeployRepo.list(anyString())).thenReturn(Collections.emptyList());
        deployer = new RulesDeployerService(mockedDeployRepo, DEPLOY_PATH);
    }

    @Test
    public void test_deploy_singleDeployment() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_deploy_singleDeployment_whenFoldersSupports() throws Exception {
        init(FolderRepository.class, false);
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
        verify((FolderRepository) mockedDeployRepo, times(1))
            .save(fileDataCaptor.capture(), fileChangesFromZipCaptor.capture(), eq(ChangesetType.FULL));
        assertNotNull(fileChangesFromZipCaptor.getValue());
        assertNotNull(fileDataCaptor.getValue());
    }

    @Test
    public void test_deploy_singleDeployment_with_custom_name() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy("customName", is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_deploy_without_description() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream(NO_NAME_DEPLOYMENT)) {
            deployer.deploy("customName",is, true);
        }
        verify(mockedDeployRepo, times(1)).save(fileDataCaptor.capture(), any(InputStream.class));
        final FileData actualFileData = fileDataCaptor.getValue();
        assertEquals("deploy/customName/customName", actualFileData.getName());
    }

    @Test
    public void test_multideploy_without_name() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream("noname-multiple-deployment.zip")) {
            deployer.deploy("customName-deployment", is, true);
        }
        List<FileItem> actualFileItems = catchDeployFileItems();
        final String baseDeploymentPath = DEPLOY_PATH + "customName-deployment/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);

        for (FileItem actualFileItem : actualFileItems) {
            Map<String, byte[]> entries =  DeploymentUtils.unzip(actualFileItem.getStream());
            if ((baseDeploymentPath + "project1").equals(actualFileItem.getData().getName())) {
                assertEquals(3, entries.size());
                assertNotNull(entries.get("rules.xml"));
                assertNotNull(entries.get("Project1-Main.xlsx"));
                assertNotNull(entries.get("rules-deploy.xml"));
            } else if ((baseDeploymentPath + "project2").equals(actualFileItem.getData().getName())) {
                assertEquals(3, entries.size());
                assertNotNull(entries.get("rules.xml"));
                assertNotNull(entries.get("rules/Project2-Main.xlsx"));
                assertNotNull(entries.get("rules-deploy.xml"));
            }
        }
    }

    @Test
    public void test_deploy_singleDeployment_whenNotOverridable() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_deploy_singleDeployment_whenNotOverridableAndDeployedAlready() throws Exception {
        init(Repository.class, false);
        when(mockedDeployRepo.list(DEPLOY_PATH + "project2/")).thenReturn(Collections.singletonList(new FileData()));
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    public void test_deploy_singleDeployment_whenOverridableAndDeployedAlready() throws Exception {
        init(Repository.class, false);
        when(mockedDeployRepo.list(DEPLOY_PATH + "project2/")).thenReturn(Collections.singletonList(new FileData()));
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    private void assertSingleDeployment(String expectedName) throws IOException {
        verify(mockedDeployRepo, times(1)).save(fileDataCaptor.capture(), any(InputStream.class));
        final FileData actualFileData = fileDataCaptor.getValue();
        assertNotNull(actualFileData);
        assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor());
        assertTrue("Content size must be greater thar 0", actualFileData.getSize() > 0);
        assertEquals(expectedName, actualFileData.getName());
    }

    @Test
    public void test_deploy_multipleDeployment() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream(MULTIPLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        List<FileItem> actualFileItems = catchDeployFileItems();
        final String baseDeploymentPath = DEPLOY_PATH + "yaml_project/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);

        for (FileItem actualFileItem : actualFileItems) {
            Map<String, byte[]> entries =  DeploymentUtils.unzip(actualFileItem.getStream());
            if ((baseDeploymentPath + "project1").equals(actualFileItem.getData().getName())) {
                assertEquals(3, entries.size());
                assertNotNull(entries.get("rules.xml"));
                assertNotNull(entries.get("Project1-Main.xlsx"));
                assertNotNull(entries.get("rules-deploy.xml"));
            } else if ((baseDeploymentPath + "project2").equals(actualFileItem.getData().getName())) {
                assertEquals(3, entries.size());
                assertNotNull(entries.get("rules.xml"));
                assertNotNull(entries.get("rules/Project2-Main.xlsx"));
                assertNotNull(entries.get("rules-deploy.xml"));
            }
        }
    }

    @Test
    public void test_EPBDS_10894() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy(is, true);
        }
        assertEPBDS_10894();
    }

    @Test
    public void test_EPBDS_10894_CustomName_mustNotApplied() throws Exception {
        init(Repository.class, false);
        try (InputStream is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy("EPBDS-10894.zip", is, true);
        }
        assertEPBDS_10894();
    }

    @Test
    public void testMultiDeploymentFolderSupport_CustomName_mustNotApplied() throws IOException, RulesDeployInputException {
        init(FolderRepository.class, true);
        try (InputStream is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy("EPBDS-10894.zip", is, true);
        }
        List<FolderItem> actualFileItems = catchDeployFolders();
        final String baseDeploymentPath = "EPBDS-10894_yaml_project/";
        assertEquals(1, actualFileItems.size());
        FolderItem folderItem = actualFileItems.get(0);
        final Set<String> actualList = StreamSupport
                .stream(folderItem.getFiles().spliterator(), false)
                .map(it -> it.getData().getName())
                .collect(Collectors.toSet());
        final Set<String> expectedFiles = toSet(baseDeploymentPath + "project1/rules.xml",
                baseDeploymentPath + "project1/Project1.xlsx",
                baseDeploymentPath + "project2/Project2.xlsx",
                baseDeploymentPath + "deployment.yaml");
        assertSetEquals(expectedFiles, actualList);
    }

    @Test
    public void testMultiDeploymentFolderSupport_NoDeploymentName() throws IOException, RulesDeployInputException {
        init(FolderRepository.class, true);
        try (InputStream is = getResourceAsStream("noname-multiple-deployment.zip")) {
            deployer.deploy("customName-deployment", is, true);
        }
        List<FolderItem> actualFileItems = catchDeployFolders();
        final String baseDeploymentPath = "customName-deployment/";
        assertEquals(1, actualFileItems.size());
        FolderItem folderItem = actualFileItems.get(0);
        final Set<String> actualList = StreamSupport
                .stream(folderItem.getFiles().spliterator(), false)
                .map(it -> it.getData().getName())
                .collect(Collectors.toSet());
        final Set<String> expectedFiles = toSet(baseDeploymentPath + "deployment.yaml",
                baseDeploymentPath + "project1/rules.xml",
                baseDeploymentPath + "project1/rules-deploy.xml",
                baseDeploymentPath + "project1/Project1-Main.xlsx",
                baseDeploymentPath + "project2/rules/Project2-Main.xlsx",
                baseDeploymentPath + "project2/rules-deploy.xml",
                baseDeploymentPath + "project2/rules.xml");
        assertSetEquals(expectedFiles, actualList);
    }

    private static void assertSetEquals(Set<String> expected, Set<String> actual) {
        Set<String> rest = new HashSet<>(expected);
        rest.removeAll(actual);
        if (!rest.isEmpty()) {
            fail(String.format("Missed expected items: %s", String.join(", ", rest)));
        }

        rest = new HashSet<>(actual);
        rest.removeAll(expected);
        if (!rest.isEmpty()) {
            fail(String.format("Unexpected items: %s", String.join(", ", rest)));
        }
    }

    private void assertEPBDS_10894() throws IOException {
        List<FileItem> actualFileItems = catchDeployFileItems();
        final String baseDeploymentPath = DEPLOY_PATH + "EPBDS-10894_yaml_project/";
        assertMultipleDeployment(toSet(baseDeploymentPath + "project1", baseDeploymentPath + "project2"),
                actualFileItems);

        for (FileItem actualFileItem : actualFileItems) {
            Map<String, byte[]> entries =  DeploymentUtils.unzip(actualFileItem.getStream());
            if ((baseDeploymentPath + "project1").equals(actualFileItem.getData().getName())) {
                assertEquals(2, entries.size());
                assertNotNull(entries.get("rules.xml"));
                assertNotNull(entries.get("Project1.xlsx"));
            } else if ((baseDeploymentPath + "project2").equals(actualFileItem.getData().getName())) {
                assertEquals(1, entries.size());
                assertNotNull(entries.get("Project2.xlsx"));
            }
        }
    }

    private List<FileItem> catchDeployFileItems() throws IOException {
        Class<List<FileItem>> listClass = (Class) List.class;
        ArgumentCaptor<List<FileItem>> captor = ArgumentCaptor.forClass(listClass);

        verify(mockedDeployRepo, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private List<FolderItem> catchDeployFolders() throws IOException {
        Class<List<FolderItem>> listClass = (Class) List.class;
        ArgumentCaptor<List<FolderItem>> captor = ArgumentCaptor.forClass(listClass);

        verify((FolderRepository) mockedDeployRepo, times(1)).save(captor.capture(), eq(ChangesetType.FULL));
        return captor.getValue();
    }

    private void assertMultipleDeployment(Set<String> expectedNames, List<FileItem> actualFileDatas) {
        assertFalse(actualFileDatas.isEmpty());
        Set<String> namesToVerify = new HashSet<>(expectedNames);
        Set<String> unexpectedNames = new HashSet<>();
        for (FileItem actualFileItem : actualFileDatas) {
            final FileData actualFileData = actualFileItem.getData();
            assertNotNull(actualFileData);
            assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor());
            assertTrue("Content size must be greater than 0", actualFileData.getSize() > 0);
            if (namesToVerify.contains(actualFileData.getName())) {
                namesToVerify.remove(actualFileData.getName());
            } else {
                unexpectedNames.add(actualFileData.getName());
            }
        }
        if (!unexpectedNames.isEmpty()) {
            fail(String.format("Unexpected deployment names: %s", String.join(", ", unexpectedNames)));
        }
        if (!namesToVerify.isEmpty()) {
            fail(String.format("Missed expected deployment names: %s", String.join(", ", namesToVerify)));
        }
    }

    private InputStream getResourceAsStream(String name) {
        return RulesDeployerServiceTest.class.getClassLoader().getResourceAsStream(name);
    }

    private Set<String> toSet(String... args) {
        return Stream.of(args)
                .collect(Collectors.toSet());
    }
}
