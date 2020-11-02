package org.openl.rules.repository.file;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class ZippedLocalRepositoryTest {

    private static final String REPOSITORY_ROOT = "target/test-zip-repository/";

    private File repositoryRoot;
    private FolderRepository repository;
    private Map<String, byte[]> singleDeployment;
    private Map<String, byte[]> multiDeployment;

    @Before
    public void setUp() throws RRepositoryException, IOException {
        this.repositoryRoot = new File(REPOSITORY_ROOT);
        FileUtils.deleteQuietly(this.repositoryRoot);
        ZippedLocalRepository repository = new ZippedLocalRepository();
        repository.setUri(REPOSITORY_ROOT);
        repository.initialize();
        this.repository = repository;
        setUpZipRepository();
    }

    @After
    public void cleanUp() throws IOException {
        ((Closeable) repository).close();
    }

    private void setUpZipRepository() throws IOException {
        Map<String, byte[]> singleDeployment = new HashMap<>();
        singleDeployment.put("rules.xml", "foo".getBytes());
        singleDeployment.put("rules/Algorithm.xlsx", "bar".getBytes());
        singleDeployment.put("rules/dir/", null);
        generateZipFile("single", "singleDeployment.zip", singleDeployment);
        this.singleDeployment = Collections.unmodifiableMap(singleDeployment);

        Map<String, byte[]> multiDeployment = new HashMap<>();
        multiDeployment.put("project1/rules.xml", "project1-foo".getBytes());
        multiDeployment.put("project1/rules/Algorithm1.xlsx", "project1-bar".getBytes());
        multiDeployment.put("project2/rules.xml", "project2-foo".getBytes());
        multiDeployment.put("project2/Algorithm2.xlsx", "project2-bar".getBytes());
        generateZipFile("", "multiDeployment.zip", multiDeployment);
        this.multiDeployment = Collections.unmodifiableMap(multiDeployment);
    }

    private void generateZipFile(String dirs, String name, Map<String, byte[]> entries) throws IOException {
        Path zipFilePath = repositoryRoot.toPath().resolve(dirs);
        if (!Files.exists(zipFilePath)) {
            Files.createDirectories(zipFilePath);
        }
        zipFilePath = zipFilePath.resolve(name);
        if (Files.exists(zipFilePath)) {
            throw new IOException("Duplicated file " + name);
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                String zipPath = entry.getKey();
                ZipEntry zipEntry = new ZipEntry(zipPath);
                zos.putNextEntry(zipEntry);
                byte[] bytes = entry.getValue();
                if (bytes != null) {
                    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
                    IOUtils.copy(baos, zos);
                }
                zos.closeEntry();
            }
        }
    }

    @Test
    public void listFoldersTest() throws IOException {
        //test deployment folders
        List<FileData> fileDataRoot = repository.listFolders("/");
        assertEquals(2, fileDataRoot.size());
        Map<String, FileData> fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFolderFileData(fileMap.get("single"));
        assertExistsFolderFileData(fileMap.get("multiDeployment.zip"));

        //test project folders
        fileDataRoot = repository.listFolders("/single");
        assertEquals(1, fileDataRoot.size());
        fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFolderFileData(fileMap.get("single/singleDeployment.zip"));

        fileDataRoot = repository.listFolders("/multiDeployment.zip");
        assertEquals(2, fileDataRoot.size());
        fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFolderFileData(fileMap.get("multiDeployment.zip/project1"));
        assertExistsFolderFileData(fileMap.get("multiDeployment.zip/project2"));
    }

    @Test
    public void listFilesTest() throws IOException {
        //test folder files
        List<FileData> fileDataRoot = repository.listFiles("/single/singleDeployment.zip", null);
        assertEquals(2, fileDataRoot.size());
        Map<String, FileData> fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFileData(fileMap.get("single/singleDeployment.zip/rules.xml"));
        assertExistsFileData(fileMap.get("single/singleDeployment.zip/rules/Algorithm.xlsx"));

        fileDataRoot = repository.listFiles("/multiDeployment.zip/project1", null);
        assertEquals(2, fileDataRoot.size());
        fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFileData(fileMap.get("multiDeployment.zip/project1/rules.xml"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project1/rules/Algorithm1.xlsx"));

        fileDataRoot = repository.listFiles("/multiDeployment.zip/project2", null);
        assertEquals(2, fileDataRoot.size());
        fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFileData(fileMap.get("multiDeployment.zip/project2/rules.xml"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project2/Algorithm2.xlsx"));
    }

    @Test
    public void listTest() throws IOException {
        List<FileData> fileDataRoot = repository.list("/");
        assertEquals(6, fileDataRoot.size());
        Map<String, FileData> fileMap = flatMap(fileDataRoot, FileData::getName);
        assertExistsFileData(fileMap.get("single/singleDeployment.zip/rules.xml"));
        assertExistsFileData(fileMap.get("single/singleDeployment.zip/rules/Algorithm.xlsx"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project1/rules.xml"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project1/rules/Algorithm1.xlsx"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project2/rules.xml"));
        assertExistsFileData(fileMap.get("multiDeployment.zip/project2/Algorithm2.xlsx"));
    }

    @Test
    public void readTest() throws IOException {
        assertSingleDeployment("/single/singleDeployment.zip/rules.xml", "rules.xml");
        assertSingleDeployment("/single/singleDeployment.zip/rules/Algorithm.xlsx", "rules/Algorithm.xlsx");
        assertMultiDeployment("/multiDeployment.zip/project1/rules.xml", "project1/rules.xml");
        assertMultiDeployment("/multiDeployment.zip/project1/rules/Algorithm1.xlsx", "project1/rules/Algorithm1.xlsx");
        assertMultiDeployment("/multiDeployment.zip/project2/rules.xml", "project2/rules.xml");
        assertMultiDeployment("/multiDeployment.zip/project2/Algorithm2.xlsx", "project2/Algorithm2.xlsx");
    }

    private void assertSingleDeployment(String repositoryPath, String name) throws IOException {
        FileItem actualFileItem = repository.read(repositoryPath);
        assertNotNull(actualFileItem);
        assertExistsFileData(actualFileItem.getData());
        assertArrayEquals(singleDeployment.get(name), read(actualFileItem.getStream()));
    }

    private void assertMultiDeployment(String repositoryPath, String name) throws IOException {
        FileItem actualFileItem = repository.read(repositoryPath);
        assertNotNull(actualFileItem);
        assertExistsFileData(actualFileItem.getData());
        assertArrayEquals(multiDeployment.get(name), read(actualFileItem.getStream()));
    }

    public byte[] read(InputStream is) throws IOException {
        byte[] array = new byte[is.available()];
        is.read(array);
        return array;
    }

    private static void assertExistsFolderFileData(FileData actual) {
        assertNotNull(actual);
        assertNotNull(actual.getModifiedAt());
        assertNotNull(actual.getVersion());
    }

    private static void assertExistsFileData(FileData actual) {
        assertNotNull(actual);
        assertNotNull(actual.getModifiedAt());
        assertNull(actual.getVersion());
        assertNotEquals(0L, actual.getSize());
    }

    public static <T, K> Map<K, T> flatMap(List<T> list, Function<T, K> key) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(key, it -> it));
    }
}
