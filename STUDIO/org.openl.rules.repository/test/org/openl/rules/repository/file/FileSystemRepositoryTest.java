package org.openl.rules.repository.file;

import static org.junit.Assert.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.openl.rules.repository.RepositoryVerifier;
import org.openl.rules.repository.api.ChangesetType;
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
        RepositoryVerifier.testRepo(repo);
        testRepository(repo);
    }

    @Test
    public void createFolderOnDemand() throws IOException {
        File base = new File("target/test-file-repository-on-demand/");
        FileUtils.deleteQuietly(base);

        try {
            File root = new File(base, "my-repo");

            FileSystemRepository repo = new FileSystemRepository();
            repo.setRoot(root);
            repo.initialize();
            assertFalse("Repo folder must not be created after initialize().", root.exists());

            RepositoryVerifier.assertList(repo, "", 0);
            assertFalse("We didn't modify repository. Repo folder must not be created.", root.exists());

            RepositoryVerifier.assertSave(repo, "folder1/text", "The file in the folder");
            assertTrue("We added the file to repo. Repo folder must exist.", root.exists());

            RepositoryVerifier.assertSave(repo, "folder2/text", "The file in the folder");
            assertTrue("We added the file to repo. Repo folder must exist.", root.exists());

            RepositoryVerifier.assertDelete(repo, "folder2/text", true);
            assertTrue("Repo still contains at least 1 file. Repo folder must exist.", root.exists());

            RepositoryVerifier.assertDelete(repo, "folder1/text", true);
            assertFalse("Repo folder must be deleted when all files in it were deleted.", root.exists());

            assertTrue("Repository's base folder must not be deleted: Repo isn't responsible for it.", base.exists());
        } finally {
            // Cleanup
            FileUtils.deleteQuietly(base);
        }
    }

    private void testRepository(FileSystemRepository repo) throws IOException {
        ZipInputStream stream = createZipInputStream("first", "second", "folder/name", "very/deep/folder/file");
        assertSaveFromZip(repo, stream);
        stream.close();

        assertSave(repo, "folder", "multiple", new Date(1234567890), "folder/file1");
        assertSave(repo, "fol/der/", "text", new Date(12345678901L), "fol/der/file1", "fol/der/file2");
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
            assertEquals("Wrong file length", text.length(), result.getSize());
            RepositoryVerifier.assertRead(repo, name, text);
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
            RepositoryVerifier.assertRead(repo, name, text);
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
