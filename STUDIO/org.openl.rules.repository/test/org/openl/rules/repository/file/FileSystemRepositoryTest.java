package org.openl.rules.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

public class FileSystemRepositoryTest {

    @TempDir
    File tmpDir;

    @Test
    public void createFolderOnDemand() throws IOException {
        File root = new File(tmpDir, "parent/my-repo");

        FileSystemRepository repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
        assertFalse(root.exists(), "Repo folder must not be created after initialize().");

        assertList(repo, "", 0);
        assertFalse(root.exists(), "We didn't modify repository. Repo folder must not be created.");

        assertSave(repo, "folder1/text", "The file in the folder");
        assertTrue(root.exists(), "We added the file to repo. Repo folder must exist.");

        assertSave(repo, "folder2/text", "The file in the folder");
        assertTrue(root.exists(), "We added the file to repo. Repo folder must exist.");

        assertDelete(repo, "folder2/text", true);
        assertTrue(root.exists(), "Repo still contains at least 1 file. Repo folder must exist.");

        assertDelete(repo, "folder1/text", true);
        assertFalse(root.exists(), "Repo folder must be deleted when all files in it were deleted.");

        assertTrue(root.getParentFile().exists(), "Repository's base folder must not be deleted: Repo is not responsible for it.");
    }

    @Test
    public void testRepo() throws IOException {
        FileSystemRepository repo = new FileSystemRepository();
        repo.setRoot(tmpDir);
        repo.initialize();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, Calendar.NOVEMBER, 25, 10, 11, 12);
        calendar.set(Calendar.MILLISECOND, 0);

        assertList(repo, "", 0);
        assertSave(repo, ".override", "The original content");
        assertSave(repo, "first.txt", "The first file in the repository");
        assertSave(repo, "second.txt", "The second file in the repository", calendar.getTime());
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

        ZipInputStream stream = createZipInputStream("first", "second", "folder/name", "very/deep/folder/file");
        assertSaveFromZip(repo, stream);
        stream.close();

        assertSave(repo, "folder", "multiple", calendar.getTime(), "folder/file1");
        assertSave(repo, "fol/der/", "text", calendar.getTime(), "fol/der/file1", "fol/der/file2");

    }

    private void assertNoRead(Repository repo, String name) throws IOException {
        FileItem result = repo.read(name);
        assertNull(result, "Null value should be returned for the absent file");
    }

    private void assertRead(Repository repo, String name, String value) throws IOException {
        try (var result = repo.read(name)) {
            assertNotNull(result, "The file is not found.");
            FileData data = result.getData();
            assertNotNull(data, "The file descriptor is missing.");
            assertEquals(name, data.getName(), "Wrong file name");
            InputStream stream = result.getStream();
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(value, text, "Unexpected content in the file.");
        }
    }

    private void assertDelete(Repository repo, String name, boolean expected) throws IOException {
        FileData fileData = new FileData();
        fileData.setName(name);
        boolean result = repo.delete(fileData);
        assertEquals(expected, result, "The deleting of the file has been failed");
    }

    private void assertList(Repository repo, String path, int size) throws IOException {
        List<FileData> list = repo.list(path);
        assertEquals(size, list.size(), "Unexpected size of the directory [" + path + "]");
    }

    private void assertSave(Repository repo, String name, String text) throws IOException {
        assertSave(repo, name, text, null);
    }

    private void assertSave(Repository repo, String name, String text, Date modifiedAt) throws IOException {
        FileData data = new FileData();
        data.setName(name);
        if (modifiedAt != null) {
            data.setModifiedAt(modifiedAt);
        }

        FileData result = repo.save(data, IOUtils.toInputStream(text));
        assertEquals(text.length(), result.getSize(), "Wrong file length");
        assertRead(repo, name, text);
        assertEquals(name, result.getName(), "Wrong file name");

        if (modifiedAt != null) {
            assertEquals(modifiedAt, result.getModifiedAt(), "Wrong modifiedAt date");
        }
    }

    private void assertSave(Repository repo,
                            String folderName,
                            String text,
                            Date modifiedAt,
                            String... fileNames) throws IOException {
        FileData folder = new FileData();
        folder.setName(folderName);
        if (modifiedAt != null) {
            folder.setModifiedAt(modifiedAt);
        }

        List<FileItem> changes = new ArrayList<>();
        for (String fileName : fileNames) {
            FileData file = new FileData();
            file.setName(fileName);
            if (modifiedAt != null) {
                file.setModifiedAt(modifiedAt);
            }

            changes.add(new FileItem(file, IOUtils.toInputStream(text)));
        }

        repo.save(folder, changes, ChangesetType.FULL);
        for (String name : fileNames) {
            FileData result = repo.check(name);
            assertEquals(text.length(), result.getSize(), "Wrong file length");
            assertRead(repo, name, text);
            assertEquals(name, result.getName(), "Wrong file name");

            if (modifiedAt != null) {
                assertEquals(modifiedAt, result.getModifiedAt(), "Wrong modifiedAt date");
            }
        }
    }

    private void assertSaveFromZip(Repository repo, ZipInputStream inputStream) throws IOException {
        ZipEntry entry;
        while ((entry = inputStream.getNextEntry()) != null) {
            String name = entry.getName();
            String text = "Text for file " + name;

            FileData data = new FileData();
            data.setName(name);
            FileData result = repo.save(data, inputStream);
            assertEquals(text.length(), result.getSize(), "Wrong file length");
            assertRead(repo, name, text);
            assertEquals(name, result.getName(), "Wrong file name");
        }
    }

    private ZipInputStream createZipInputStream(String... names) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ZipOutputStream outputStream = new ZipOutputStream(byteStream);
        for (String name : names) {
            ZipEntry entry = new ZipEntry(name);
            outputStream.putNextEntry(entry);
            String text = "Text for file " + name;
            InputStream input = IOUtils.toInputStream(text);
            input.transferTo(outputStream);
            outputStream.closeEntry();
        }
        outputStream.close();
        return new ZipInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
    }

}
