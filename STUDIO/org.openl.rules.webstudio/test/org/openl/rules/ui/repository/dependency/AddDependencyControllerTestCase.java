package org.openl.rules.ui.repository.dependency;

import junit.framework.TestCase;
import org.openl.rules.workspace.abstracts.ProjectVersion;

public class AddDependencyControllerTestCase extends TestCase {
    public void testVersionFromString() {
        String[] examples = {
                "1", "1.", "0.4", "0.4.", "2.1.2", "2.1.4.", "21.22.123"
        };
        int[][] expected = {
                {1, 0, 0},
                {1, 0, 0},
                {0, 4, 0},
                {0, 4, 0},
                {2, 1, 2},
                {2, 1, 4},
                {21, 22, 123}
        };

        for (int i = 0; i < examples.length; i++) {
            ProjectVersion projectVersion = AddDependencyController.versionFromString(examples[i]);
            assertNotNull("got null version", projectVersion);
            assertEquals("incorrect major number", expected[i][0], projectVersion.getMajor());
            assertEquals("incorrect minor number", expected[i][1], projectVersion.getMinor());
            assertEquals("incorrect revision number", expected[i][2], projectVersion.getRevision());
        }
    }

    public void testVersionFromStringFail() {
        String[] examples = {
                ".1", "1.-1", "-2", "0..4", "2.1.2..", "1.1.1.1", "", "."
        };

        for (String s : examples) {
            assertNull("null expected", AddDependencyController.versionFromString(s));
        }
        
    }
}
