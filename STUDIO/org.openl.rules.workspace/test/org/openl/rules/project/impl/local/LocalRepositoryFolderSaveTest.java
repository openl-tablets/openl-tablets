package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

/**
 * Verifies the modification bookkeeping of a folder changeset. The owning project must be marked
 * dirty even when the changeset targets a subfolder, and the subfolder path itself must not be
 * recorded as a project.
 *
 * @author Yury Molchan
 */
class LocalRepositoryFolderSaveTest {

    @TempDir
    Path root;

    private static FileItem item(String name, String content) {
        var data = new FileData();
        data.setName(name);
        return new FileItem(data, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void subfolderChangesetMarksTheOwningProjectDirty() throws Exception {
        var registry = MetainfoRegistry.open(root);
        var repository = new LocalRepository(root, registry);

        var folderData = new FileData();
        folderData.setName("Project1/sub");
        repository.save(folderData, List.of(item("Project1/sub/a.txt", "A")), ChangesetType.FULL);

        assertTrue(registry.isDirty("Project1"), "the owning project must be marked dirty");
        assertFalse(registry.isDirty("Project1/sub"), "the folder path must not be recorded as a project");

        // The bookkeeping under the folder keeps working, e.g. for a later delete.
        var deleteData = new FileData();
        deleteData.setName("Project1/sub/a.txt");
        assertTrue(repository.delete(deleteData));
    }

    @Test
    void projectRootChangesetIsMarkedTheSameWay() throws Exception {
        var registry = MetainfoRegistry.open(root);
        var repository = new LocalRepository(root, registry);

        var folderData = new FileData();
        folderData.setName("Project1");
        repository.save(folderData, List.of(item("Project1/b.txt", "B")), ChangesetType.DIFF);

        assertTrue(registry.isDirty("Project1"), "the project must be marked dirty");
    }
}
