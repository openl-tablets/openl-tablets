package org.openl.rules.ruleservice.deployer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

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
        assertSingleDeployment();
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
        assertSingleDeployment();
    }

    @Test
    public void test_deploy_singleDeployment_whenNotOverridable() throws Exception {
        try (InputStream is = getResourceAsStream(SINGLE_DEPLOYMENT)) {
            deployer.deploy(is, false);
        }
        assertSingleDeployment();
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
        assertSingleDeployment();
    }

    private void assertSingleDeployment() throws IOException {
        verify(mockedDeployRepo, times(1)).save(fileDataCaptor.capture(), any(InputStream.class));
        final FileData actualFileData = fileDataCaptor.getValue();
        assertNotNull(actualFileData);
        assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor());
        assertTrue("Content size must be greater thar 0", actualFileData.getSize() > 0);
        final String expectedName = DEPLOY_PATH + "project2/Rules";
        assertEquals(expectedName, actualFileData.getName());
    }

    @Test
    public void test_deploy_multipleDeployment() throws Exception {
        try (InputStream is = getResourceAsStream(MULTIPLE_DEPLOYMENT)) {
            deployer.deploy(is, true);
        }
        assertMultipleDeployment();
    }

    private void assertMultipleDeployment() throws IOException {
        Class<List<FileItem>> listClass = (Class) List.class;
        ArgumentCaptor<List<FileItem>> captor = ArgumentCaptor.forClass(listClass);

        verify(mockedDeployRepo, times(1)).save(captor.capture());
        List<FileItem> actualFileDatas = captor.getValue();

        assertFalse(actualFileDatas.isEmpty());
        for (FileItem actualFileItem : actualFileDatas) {
            final FileData actualFileData = actualFileItem.getData();
            assertNotNull(actualFileData);
            assertEquals(RulesDeployerService.DEFAULT_AUTHOR_NAME, actualFileData.getAuthor());
            assertTrue("Content size must be greater thar 0", actualFileData.getSize() > 0);
            final String expectedName = DEPLOY_PATH + "yaml_project/project";
            assertTrue(actualFileData.getName().startsWith(expectedName));
            assertEquals(expectedName.length() + 1, actualFileData.getName().length());
        }
    }

    private InputStream getResourceAsStream(String name) {
        return RulesDeployerServiceTest.class.getClassLoader().getResourceAsStream(name);
    }
}
