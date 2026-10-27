package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.repository.git.TestGitUtils.createFileData;
import static org.openl.rules.repository.git.TestGitUtils.createLocalRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileItem;
import org.openl.util.IOUtils;

/**
 * A full save of a folder keeps the ignored files it holds: no one in OpenL Studio sees them to save them again.
 */
class IgnoredPathsSaveTest {

    @TempDir
    private File root;
    @TempDir
    private File localRepositoriesFolder;
    @AutoClose
    private GitRepository repo;

    @Test
    void aFullSaveKeepsTheIgnoredFilesOfTheFolder() throws IOException {
        repo = createLocalRepository(new File(root, "design-repository"), localRepositoriesFolder, "**/pom.xml");
        repo.save(createFileData("rules/Rating", ""),
                List.of(new FileItem("rules/Rating/rules.xml", IOUtils.toInputStream("<project/>")),
                        new FileItem("rules/Rating/Old.xlsx", IOUtils.toInputStream("old")),
                        new FileItem("rules/Rating/pom.xml", IOUtils.toInputStream("1.0.0"))),
                ChangesetType.DIFF);

        repo.save(createFileData("rules/Rating", ""),
                List.of(new FileItem("rules/Rating/rules.xml", IOUtils.toInputStream("<project><name/></project>"))),
                ChangesetType.FULL);

        assertEquals("1.0.0", GitRepositoryTest.readText(repo.read("rules/Rating/pom.xml")));
        assertEquals("<project><name/></project>", GitRepositoryTest.readText(repo.read("rules/Rating/rules.xml")));
        assertNull(repo.read("rules/Rating/Old.xlsx"));
    }
}
