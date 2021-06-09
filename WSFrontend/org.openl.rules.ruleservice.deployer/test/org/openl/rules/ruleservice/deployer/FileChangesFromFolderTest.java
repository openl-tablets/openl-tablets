package org.openl.rules.ruleservice.deployer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;

public class FileChangesFromFolderTest {

    @Test
    public void testIterator() throws IOException {
        Map<String, byte[]> actualEntries = executeIterator("test-resources/single-deployment.zip", "single-deployment");
        assertEquals(3, actualEntries.size());
        assertSizeEquals(8235, actualEntries.get("single-deployment/rules/Project2-Main.xlsx"));
        assertSizeEquals(191, actualEntries.get("single-deployment/rules.xml"));
        assertSizeEquals(290, actualEntries.get("single-deployment/rules-deploy.xml"));
    }

    @Test
    public void testIterator2() throws IOException {
        Map<String, byte[]> actualEntries = executeIterator("test-resources/multiple-deployment.zip", "multiple-deployment");
        assertEquals(7, actualEntries.size());
        assertSizeEquals(23, actualEntries.get("multiple-deployment/deployment.yaml"));
        assertSizeEquals(290, actualEntries.get("multiple-deployment/project1/rules-deploy.xml"));
        assertSizeEquals(8403, actualEntries.get("multiple-deployment/project1/Project1-Main.xlsx"));
        assertSizeEquals(318, actualEntries.get("multiple-deployment/project1/rules.xml"));
        assertSizeEquals(8235, actualEntries.get("multiple-deployment/project2/rules/Project2-Main.xlsx"));
        assertSizeEquals(191, actualEntries.get("multiple-deployment/project2/rules.xml"));
        assertSizeEquals(290, actualEntries.get("multiple-deployment/project2/rules-deploy.xml"));
    }

    private static void assertSizeEquals(int expectedSize, byte[] arr) {
        assertNotNull(arr);
        assertEquals(expectedSize, arr.length);
    }

    private static Map<String, byte[]> executeIterator(String pathToArchive, String folderTo) throws IOException {
        Map<String, byte[]> actualEntries = new HashMap<>();
        try (FileSystem fs = openFileSystem(pathToArchive)) {
            try (FileChangesFromFolder changes = new FileChangesFromFolder(fs.getPath("/"), folderTo)) {
                for (FileItem item : changes) {
                    try (InputStream source = item.getStream()) {
                        ByteArrayOutputStream target = new ByteArrayOutputStream();
                        IOUtils.copy(source, target);
                        actualEntries.put(item.getData().getName(), target.toByteArray());
                    }
                }
            }
        }
        return actualEntries;
    }

    private static FileSystem openFileSystem(String archive) throws IOException {
        return FileSystems.newFileSystem(Paths.get(archive), Thread.currentThread().getContextClassLoader());
    }

}
