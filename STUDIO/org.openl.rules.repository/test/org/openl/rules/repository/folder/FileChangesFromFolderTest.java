package org.openl.rules.repository.folder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;

public class FileChangesFromFolderTest {

    @Test
    public void testIterator() throws IOException {
        Map<String, byte[]> actualEntries = executeIterator("test-resources/archive.zip", FileChangesFromFolder::new);
        assertEquals(7, actualEntries.size());
        assertSizeEquals(23, actualEntries.get("/deployment.yaml"));
        assertSizeEquals(290, actualEntries.get("/project1/rules-deploy.xml"));
        assertSizeEquals(8403, actualEntries.get("/project1/Project1-Main.xlsx"));
        assertSizeEquals(318, actualEntries.get("/project1/rules.xml"));
        assertSizeEquals(8235, actualEntries.get("/project2/rules/Project2-Main.xlsx"));
        assertSizeEquals(191, actualEntries.get("/project2/rules.xml"));
        assertSizeEquals(290, actualEntries.get("/project2/rules-deploy.xml"));
    }

    @Test
    public void testIterator2() throws IOException {
        Map<String, byte[]> actualEntries = executeIterator("test-resources/archive.zip",
            root -> new FileChangesFromFolder(root,
                "/root-folder",
                path -> !Objects.equals(path.toString(), "/deployment.yaml"),
                new FileAdaptor() {
                    @Override
                    public boolean accept(Path path) {
                        return Objects.equals(path.toString(), "/project2/rules.xml");
                    }

                    @Override
                    public InputStream apply(InputStream inputStream) {
                        return new ByteArrayInputStream(
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus ut magna quam."
                                .getBytes(StandardCharsets.UTF_8));
                    }
                }));

        assertEquals(6, actualEntries.size());
        assertSizeEquals(290, actualEntries.get("/root-folder/project1/rules-deploy.xml"));
        assertSizeEquals(8403, actualEntries.get("/root-folder/project1/Project1-Main.xlsx"));
        assertSizeEquals(318, actualEntries.get("/root-folder/project1/rules.xml"));
        assertSizeEquals(8235, actualEntries.get("/root-folder/project2/rules/Project2-Main.xlsx"));
        assertSizeEquals(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus ut magna quam."
                .getBytes(StandardCharsets.UTF_8).length,
            actualEntries.get("/root-folder/project2/rules.xml"));
        assertSizeEquals(290, actualEntries.get("/root-folder/project2/rules-deploy.xml"));
    }

    private static void assertSizeEquals(int expectedSize, byte[] arr) {
        assertNotNull(arr);
        assertEquals(expectedSize, arr.length);
    }

    private static Map<String, byte[]> executeIterator(String pathToArchive,
            FileChangesFactory factory) throws IOException {
        Map<String, byte[]> actualEntries = new HashMap<>();
        try (FileSystem fs = openFileSystem(pathToArchive)) {
            try (FileChangesFromFolder changes = factory.create(fs.getPath("/"))) {
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

    private interface FileChangesFactory {

        FileChangesFromFolder create(Path root) throws IOException;

    }

    private static FileSystem openFileSystem(String archive) throws IOException {
        return FileSystems.newFileSystem(Paths.get(archive), Thread.currentThread().getContextClassLoader());
    }

}
