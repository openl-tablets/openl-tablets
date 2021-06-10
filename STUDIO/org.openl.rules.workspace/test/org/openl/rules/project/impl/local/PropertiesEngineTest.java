package org.openl.rules.project.impl.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.openl.util.FileUtils;

public class PropertiesEngineTest {
    private final File ROOT = new File("target/test-file-repository/");

    @After
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
}