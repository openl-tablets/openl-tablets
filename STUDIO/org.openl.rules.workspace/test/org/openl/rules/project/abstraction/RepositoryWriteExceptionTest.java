package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.file.FileSystemRepository;

/**
 * What a project tells about a repository that cannot store what the project writes into it.
 *
 * @author Yury Molchan
 */
class RepositoryWriteExceptionTest {

    @TempDir
    Path localRoot;

    @TempDir
    Path designRoot;

    private AProject local;

    @BeforeEach
    void init() throws IOException {
        Files.createDirectories(localRoot.resolve("Project"));
        Files.writeString(localRoot.resolve("Project/rules.xml"), "<project/>");
        var repository = new FileSystemRepository();
        repository.setRoot(localRoot);
        repository.initialize();
        local = new AProject(repository, "Project");
    }

    @Test
    void namesTheRepositoryThatCannotStoreAFolder() {
        var design = designProject(new FileSystemRepository() {
            @Override
            public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType)
                    throws IOException {
                throw new IOException("The disk is full.");
            }
        });

        var refused = assertThrows(RepositoryWriteException.class, () -> design.update(local, null));

        assertEquals("Design", refused.getRepositoryName());
        assertEquals("The disk is full.", refused.getCause().getMessage());
    }

    @Test
    void namesTheRepositoryThatCannotStoreAnArchive() {
        var design = designProject(new FileSystemRepository() {
            @Override
            public FileData save(FileData data, InputStream stream) throws IOException {
                throw new IOException("Connection is not available.");
            }

            @Override
            public Features supports() {
                // A database keeps a project as an archive.
                return new FeaturesBuilder(this).build();
            }
        });

        var refused = assertThrows(RepositoryWriteException.class, () -> design.update(local, null));

        assertEquals("Design", refused.getRepositoryName());
    }

    private AProject designProject(FileSystemRepository repository) {
        repository.setRoot(designRoot);
        repository.setId("design");
        repository.setName("Design");
        repository.initialize();
        var fileData = new FileData();
        fileData.setName("Project");
        return new AProject(repository, fileData);
    }
}
