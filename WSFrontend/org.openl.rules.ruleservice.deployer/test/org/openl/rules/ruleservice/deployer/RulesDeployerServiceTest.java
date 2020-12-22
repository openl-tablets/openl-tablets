package org.openl.rules.ruleservice.deployer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.folder.FileChangesFromZip;

public class RulesDeployerServiceTest {

    private static String MULTIPLE_DEPLOYMENT = "multiple-deployment.zip";
    private static String SINGLE_DEPLOYMENT = "single-deployment.zip";
    private static String DEPLOY_PATH = "deploy/";

    private Repository mockedDeployRepo;
    private RulesDeployerService deployer;
    private ArgumentCaptor<FileData> fileDataCaptor;
    private ArgumentCaptor<FileChangesFromZip> fileChangesFromZipCaptor;

    @Before
    public void setUp() throws IOException {
        mockedDeployRepo = mock(Repository.class);
        when(mockedDeployRepo.supports()).thenReturn(new FeaturesBuilder(mockedDeployRepo).build());
        when(mockedDeployRepo.list(anyString())).thenReturn(Collections.emptyList());
        fileDataCaptor = ArgumentCaptor.forClass(FileData.class);
        fileChangesFromZipCaptor = ArgumentCaptor.forClass(FileChangesFromZip.class);

        deployer = new RulesDeployerService(mockedDeployRepo, DEPLOY_PATH);
    }

    @Test
    public void test_deploy_singleDeployment() throws Exception {
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_deploy_singleDeployment_whenFoldersSupports() throws Exception {
        FolderRepository mockedDeployRepo = mock(FolderRepository.class);
        when(mockedDeployRepo.supports())
            .thenReturn(new FeaturesBuilder(mockedDeployRepo).setMappedFolders(true).build());

        RulesDeployerService deployer = new RulesDeployerService(mockedDeployRepo, DEPLOY_PATH);
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
        verify(mockedDeployRepo, times(1))
            .save(fileDataCaptor.capture(), fileChangesFromZipCaptor.capture(), eq(ChangesetType.FULL));
        assertNotNull(fileChangesFromZipCaptor.getValue());
        assertNotNull(fileDataCaptor.getValue());
    }

    @Test
    public void test_deploy_singleDeployment_with_custom_name() throws Exception {
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy("customName", is, true);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_multideploy_without_name() throws Exception {
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
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        assertSingleDeployment(DEPLOY_PATH + "project2/project2");
    }

    @Test
    public void test_deploy_singleDeployment_whenNotOverridableAndDeployedAlready() throws Exception {
        when(mockedDeployRepo.list(DEPLOY_PATH + "project2/")).thenReturn(Collections.singletonList(new FileData()));
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        verify(mockedDeployRepo, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    public void test_deploy_singleDeployment_whenOverridableAndDeployedAlready() throws Exception {
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
        try (InputStream is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy(is, true);
        }
        assertEPBDS_10894();
    }

    @Test
    public void test_EPBDS_10894_CustomName_mustNotApplied() throws Exception {
        try (InputStream is = getResourceAsStream("EPBDS-10894.zip")) {
            deployer.deploy("EPBDS-10894.zip", is, true);
        }
        assertEPBDS_10894();
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
