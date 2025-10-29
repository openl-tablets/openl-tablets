package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;

final class TestGitUtils {
    private TestGitUtils() {
    }

    static FileData createFileData(String path, String text) {
        return createFileData(path, text, "Comment for " + path);
    }

    static FileData createFileData(String path, String text, String comment) {
        FileData fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment(comment);
        fileData.setAuthor(new UserInfo("jsmith", "jsmith@email", "John Smith"));
        return fileData;
    }

    static File createNewFile(File parent, String fileName, String text) throws IOException {
        if (!parent.mkdirs() && !parent.exists()) {
            throw new IOException("Could not create folder " + parent);
        }
        File file = new File(parent, fileName);
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + file.getAbsolutePath());
        }
        writeText(file, text);
        return file;
    }

    static void writeText(File file, String text) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.displayName())) {
            writer.append(text);
        }
    }

    static void assertContains(List<FileData> files, String fileName) {
        boolean contains = false;
        for (FileData file : files) {
            if (fileName.equals(file.getName())) {
                contains = true;
                break;
            }
        }

        assertTrue(contains, "Files list does not contain the file '" + fileName + "'");
    }
    
    static GitRootFactory mockGitRootFactory(String repositoryId, String uri, File gitRoot, String localRepositoriesFolder, boolean remote, boolean empty) throws IOException {
        GitRootFactory gitRootFactory = mock(GitRootFactory.class);
        when(gitRootFactory.create(repositoryId, uri, localRepositoriesFolder)).thenReturn(new GitRoot(remote, gitRoot, empty));
        return gitRootFactory;
    }
}
