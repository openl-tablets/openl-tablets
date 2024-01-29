package org.openl.rules.project.impl.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.util.FileUtils;

public class PropertiesEngineTest {
    private final File ROOT = new File("target/test-file-repository/");

    @AfterEach
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(ROOT);
        assertFalse(ROOT.exists());
    }

    @Test
    public void testRelativePath() {
        final File repo = new File(ROOT, "repo");

        final PropertiesEngine engine = new PropertiesEngine(repo);
        final File folder1 = new File(repo, "folder1");
        final String absolutePath = folder1.getAbsolutePath();

        // Repo folder doesn't exist
        assertEquals("folder1", engine.getRelativePath(absolutePath));

        // Create repo folder but not internal folder
        assertTrue(repo.mkdirs());
        assertEquals("folder1", engine.getRelativePath(absolutePath));

        // Create internal folder
        assertTrue(folder1.mkdirs());
        assertEquals("folder1", engine.getRelativePath(absolutePath));
    }

    @Test
    public void dontRecreateDeletedProject() {
        final File repo = new File(ROOT, "repo");
        final PropertiesEngine engine = new PropertiesEngine(repo);

        // Check that if project folder exists, we can create properties file for it.
        final File folder1 = new File(repo, "folder1");
        assertTrue(folder1.mkdirs());
        assertTrue(engine.createPropertiesFile(folder1.getAbsolutePath(), ".version").exists());

        // Check that if project folder was deleted, we don't recreate that folder and don't create properties file for
        // it.
        FileUtils.deleteQuietly(folder1);
        try {
            engine.createPropertiesFile(folder1.getAbsolutePath(), ".version");
            fail("We shouldn't recreate deleted folder");
        } catch (Exception e) {
            assertEquals("Folder '" + folder1.getPath() + "' is absent.", e.getMessage());
        }

        assertFalse(folder1.exists(), "We shouldn't recreate deleted folder");
    }
}