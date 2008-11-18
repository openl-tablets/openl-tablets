package org.openl.rules.ruleservice.publish;

import junit.framework.TestCase;
import static org.openl.rules.ruleservice.publish.FileSystemWalker.*;

/**
 * @author Aliaksandr Antonik.
 */
public class FileSystemWalkerTest extends TestCase {
    public void testChageExtension() {
        assertEquals("input.txt", changeExtension("input.rtf", "txt"));
        assertEquals("input.txt", changeExtension("input", "txt"));
        assertEquals("input.txt", changeExtension("input.", "txt"));
    }

    public void testRemoveExtension() {
        assertEquals("input", removeExtension("input.rtf"));
        assertEquals("input", removeExtension("input."));
        assertEquals("input", removeExtension("input"));
    }
}
