package org.openl.rules.webstudio.repository.dependency;

import junit.framework.TestCase;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.webstudio.web.repository.DependencyController;

public class DependencyControllerTestCase extends TestCase {
    public void testVersionFromString() {
        String[] examples = { "1", "1.", "0.4", "0.4.", "2.1.2", "2.1.4.", "21.22.123" };
        int[][] expected = { { 1, 0, 0 }, { 1, 0, 0 }, { 0, 4, 0 }, { 0, 4, 0 }, { 2, 1, 2 }, { 2, 1, 4 },
                { 21, 22, 123 } };

        for (int i = 0; i < examples.length; i++) {
            ProjectVersion projectVersion = DependencyController.versionFromString(examples[i]);
            assertNotNull("got null version", projectVersion);
            assertEquals("incorrect revision number", expected[i][2], projectVersion.getRevision());
        }
    }
/*
    public void testVersionFromStringFail() {
        String[] examples = { ".1", "0..4", "2.1.2..", "1.1.1.1", "", "." };

        for (String s : examples) {
            assertNull("null expected", DependencyController.versionFromString(s));
        }

    }
    */
}
