package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The protected and the hidden files of a project, told by their paths from the root of the project.
 *
 * @author Yury Molchan
 */
class ProjectFilesTest {

    private static final ProjectFiles FILES = ProjectFiles.of("pom.xml", "rules.xml, rules-deploy.xml");

    @Test
    void aProtectedFileIsHiddenToo() {
        assertTrue(FILES.isProtected("pom.xml"));
        assertTrue(FILES.isHidden("pom.xml"));
    }

    @Test
    void aHiddenFileIsNotProtected() {
        assertTrue(FILES.isHidden("rules-deploy.xml"));
        assertFalse(FILES.isProtected("rules-deploy.xml"));
    }

    @Test
    void aFileIsMatchedFromTheRootOfItsProject() {
        assertFalse(FILES.isProtected("rules/pom.xml"));
        assertFalse(FILES.isHidden("rules/rules.xml"));
        assertFalse(FILES.isHidden("rules/Rules.xlsx"));
    }

    @Test
    void noFileIsKeptApartWithoutSettings() {
        assertFalse(ProjectFiles.of(null, null).isProtected("pom.xml"));
        assertFalse(ProjectFiles.of(null, null).isHidden("rules.xml"));
    }
}
