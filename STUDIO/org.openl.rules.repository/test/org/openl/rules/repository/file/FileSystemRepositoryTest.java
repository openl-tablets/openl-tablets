package org.openl.rules.repository.file;

import static org.junit.Assert.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class FileSystemRepositoryTest {
    @Test
    public void test() throws Exception {
        File root = new File("target/test-file-repository/");
        FileUtils.deleteQuietly(root);
        FileSystemRepository repo = new FileSystemRepository();
        repo.setRoot(root);
        repo.initialize();
        testRepo(repo);
    }

    private void testRepo(FileSystemRepository repo) throws IOException {
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
        assertNull("Null value should be returned for the absent file", result);
    }

    private void assertRead(Repository repo, String name, String value) throws IOException {
        FileItem result = repo.read(name);
        assertNotNull("The file is not found!", result);
        FileData data = result.getData();
        assertNotNull("The file descriptor is missing!", data);
        assertEquals("Wrong file name", name, data.getName());
        InputStream stream = result.getStream();
        String text = IOUtils.toStringAndClose(stream);
        assertEquals("Unexpected content in the file.", value, text);
    }

    private void assertDelete(Repository repo, String name, boolean expected) {
        FileData fileData = new FileData();
        fileData.setName(name);
        boolean result = repo.delete(fileData);
        assertEquals("The deleting of the file has been failed", expected, result);
    }

    private void assertList(Repository repo, String path, int size) throws IOException {
        List<FileData> list = repo.list(path);
        assertEquals("Unexpected size of the directory [" + path + "]", size, list.size());
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
        assertEquals("Wrong file length", text.length(), result.getSize());
        assertRead(repo, name, text);
        assertEquals("Wrong file name", name, result.getName());

        if (modifiedAt != null) {
            assertEquals("Wrong modifiedAt date", modifiedAt, result.getModifiedAt());
        }
    }

    private void assertSave(FolderRepository repo,
            String folderName,
            String text,
            Date modifiedAt,
            String... fileNames) throws IOException {
        FileData folder = new FileData();
        folder.setName(folderName);
        if (modifiedAt != null) {
            folder.setModifiedAt(modifiedAt);
        }

        List<FileChange> changes = new ArrayList<>();
        for (String fileName : fileNames) {
            FileData file = new FileData();
            file.setName(fileName);
            if (modifiedAt != null) {
                file.setModifiedAt(modifiedAt);
            }

            changes.add(new FileChange(file, IOUtils.toInputStream(text)));
        }

        repo.save(folder, changes, ChangesetType.FULL);
        for (String name : fileNames) {
            FileData result = repo.check(name);
            assertEquals("Wrong file length", text.length(), result.getSize());
            assertRead(repo, name, text);
            assertEquals("Wrong file name", name, result.getName());

            if (modifiedAt != null) {
                assertEquals("Wrong modifiedAt date", modifiedAt, result.getModifiedAt());
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
            assertEquals("Wrong file length", text.length(), result.getSize());
            assertRead(repo, name, text);
            assertEquals("Wrong file name", name, result.getName());
        }
    }

    private ZipInputStream createZipInputStream(String... names) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ZipOutputStream outputStream = new ZipOutputStream(byteStream);
        for (String name : names) {
            ZipEntry entry = new ZipEntry(name);
            outputStream.putNextEntry(entry);
            String text = "Text for file " + name;
            IOUtils.copy(IOUtils.toInputStream(text), outputStream);
            outputStream.closeEntry();
        }
        outputStream.close();
        return new ZipInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
    }

}
