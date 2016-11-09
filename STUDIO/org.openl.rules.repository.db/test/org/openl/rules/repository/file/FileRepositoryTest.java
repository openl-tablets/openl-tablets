package org.openl.rules.repository.file;

import org.junit.Test;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileRepositoryTest {
    @Test
    public void test() throws IOException {
        File root = new File("target/test-file-repository/");
        FileUtils.deleteQuietly(root);
        FileRepository repo = new FileRepository(root);
        repo.initialize();
        testRepo(repo);
    }

    private void testRepo(Repository repo) throws IOException {
        assertList(repo, "/", 0);
        assertSave(repo, ".override", "The original content");
        assertSave(repo, "first.txt", "The first file in the repository");
        assertSave(repo, "second.txt", "The second file in the repository");
        assertSave(repo, "third#3", "The third file");
        assertSave(repo, "folder1/text", "The file in the folder");
        assertSave(repo, "very/very/very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text2", "The file in the deep folder");
        assertSave(repo, "very/deep/folder/text", "The overridden file in the deep folder");
        assertSave(repo, ".override", "This new content");
        assertList(repo, "/", 9);
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
        assertDelete(repo, "deep/deep/deep/deep/folder/exist", true);
        assertSave(repo, "deep/deep/deep", "Should be able to save after deleting empty folders");
        assertDelete(repo, "deep/deep", false);
        assertList(repo, "/", 11);
    }

    private void assertDelete(Repository repo, String name, boolean expected) {
        boolean result = repo.delete(name);
        assertEquals("The deleting of the file has been filed", expected, result);
    }

    private void assertList(Repository repo, String path, int size) {
        List<FileData> list = repo.list(path);
        assertEquals("Unexpected size of the directory [" + path + "]", size, list.size());
    }

    private void assertSave(Repository repo, String name, String text) throws IOException {
        FileData data = new FileData();
        data.setName(name);
        long startTime = System.currentTimeMillis();
        FileData result = repo.save(data, IOUtils.toInputStream(text));
        assertEquals("Wrong file length", text.length(), result.getSize());
        long modified = result.getModifiedAt().getTime();
        assertTrue("Unexpected time of modification (early).", modified >= startTime);
        assertTrue("Unexpected time of modification (late).", modified <= System.currentTimeMillis());
        assertEquals("Wrong file name",name, result.getName());
    }
}
