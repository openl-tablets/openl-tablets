package org.openl.rules.ruleservice.resolver;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class OpenLWrapperRecognizerTest {
    @Test
    public void testGetNameWithoutPostfix() {
        OpenLWrapperRecognizer recognizer = new OpenLWrapperRecognizer(new File("baseFolder/"));
        assertEquals("org.openl.tut1", recognizer.getNameWithoutPostfix(new File(
                "baseFolder/org/openl/tut1Wrapper.java"), "Wrapper.java"));
        assertEquals(null, recognizer.getNameWithoutPostfix(new File("baseFolder/org/openl/tut1.java"), "Wrapper.java"));
    }

    @Test
    public void testSubtractFilePathes() {
        assertEquals(null, OpenLWrapperRecognizer.subtractFilePathes(new File("childFolder/"), new File(
                "baseFolder/childFolder/file.ext")));
        assertEquals("file.ext", OpenLWrapperRecognizer.subtractFilePathes(new File("baseFolder/childFolder/"),
                new File("baseFolder/childFolder/file.ext")));
        assertEquals("subFolder" + File.separator + "file.ext", OpenLWrapperRecognizer.subtractFilePathes(new File(
                "baseFolder/childFolder/"), new File("baseFolder/childFolder/subFolder/file.ext")));
        assertEquals(null, OpenLWrapperRecognizer.subtractFilePathes(new File("baseFolder/childFolder/"), new File(
                "/baseFolder/childFolder/file.ext")));
    }
}
