package org.openl.rules.repository;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RepositoryVerifier {

    /**
     * Checks default scenario for all implementation of {@linkplain Repository} API.
     */
    public static void testRepo(Repository repo) throws IOException {
        assertList(repo, "", 0);
        assertSave(repo, ".override", "The original content");
        assertSave(repo, "first.txt", "The first file in the repository");
        assertSave(repo, "second.txt", "The second file in the repository", new Date(9876543210L));
        assertSave(repo, "third#3", "The third file");
        assertSave(repo, "folder1/text", "The file in the folder");
        assertSave(repo, "very/very/very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text2", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text", "The overridden file in the deep folder");
        assertRead(repo, ".override", "The original content");
        assertSave(repo, ".override", "This new content");
        assertRead(repo, ".override", "This new content");
        assertList(repo, "", 9);
        assertList(repo, "very/deep/folder/", 2);
        assertList(repo, "very/", 4);
        assertList(repo, "folder1/text", 0);
        assertList(repo, "absent/", 0);
        assertDelete(repo, "absent", false);
        assertSave(repo, ".exist", "should be deleted");
        assertDelete(repo, ".exist", true);
        assertDelete(repo, ".exist", false);
        assertSave(repo, ".exist", "Check writing after deleting");
        assertSave(repo, "deep/deep/deep/deep/folder/exist", "Should be deleted with empty folders");
        assertRead(repo, "deep/deep/deep/deep/folder/exist", "Should be deleted with empty folders");
        assertDelete(repo, "deep/deep/deep/deep/folder/exist", true);
        assertNoRead(repo, "deep/deep/deep/deep/folder/exist");
        assertSave(repo, "deep/deep/deep", "Should be able to save after deleting empty folders");
        assertDelete(repo, "deep/deep", true);
        assertList(repo, "", 10);
        assertNoRead(repo, "absent");
        assertRead(repo, ".override", "This new content");
    }

    private static void assertNoRead(Repository repo, String name) throws IOException {
        FileItem result = repo.read(name);
        assertNull("Null value should be returned for the absent file", result);
    }

    public static void assertRead(Repository repo, String name, String value) throws IOException {
        FileItem result = repo.read(name);
        assertNotNull("The file is not found.", result);
        FileData data = result.getData();
        assertNotNull("The file descriptor is missing.", data);
        assertEquals("Wrong file name", name, data.getName());
        InputStream stream = result.getStream();
        String text = IOUtils.toStringAndClose(stream);
        assertEquals("Unexpected content in the file.", value, text);
    }

    public static void assertDelete(Repository repo, String name, boolean expected) throws IOException {
        FileData fileData = new FileData();
        fileData.setName(name);
        boolean result = repo.delete(fileData);
        assertEquals("The deleting of the file has been failed", expected, result);
    }

    public static void assertList(Repository repo, String path, int size) throws IOException {
        List<FileData> list = repo.list(path);
        assertEquals("Unexpected size of the directory [" + path + "]", size, list.size());
    }

    public static void assertSave(Repository repo, String name, String text) throws IOException {
        assertSave(repo, name, text, null);
    }

    private static void assertSave(Repository repo, String name, String text, Date modifiedAt) throws IOException {
        FileData data = new FileData();
        data.setName(name);
        if (modifiedAt != null) {
            data.setModifiedAt(modifiedAt);
        }

        FileData result = repo.save(data, IOUtils.toInputStream(text));
        if (result.getSize() != -1) {
            assertEquals("Wrong file length", text.length(), result.getSize());
        }
        assertRead(repo, name, text);
        assertEquals("Wrong file name", name, result.getName());

        if (modifiedAt != null) {
            assertEquals("Wrong modifiedAt date", modifiedAt, result.getModifiedAt());
        }
    }
}
