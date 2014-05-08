package org.openl.rules.webstudio.web.repository.project;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author nsamatov.
 */
public class PredefinedTemplatesResolverTest extends TemplatesResolverTest {
    @Test
    public void testGetCategories() throws Exception {
        assertArrayEquals(new String[]{"templates", "examples", "tutorials"}, new PredefinedTemplatesResolver().getCategories());
    }

    @Test
    public void testGetTemplates() throws Exception {
        PredefinedTemplatesResolver templatesResolver = new PredefinedTemplatesResolver();

        assertArrayEquals(new String[]{"Empty Project", "Sample Project"}, templatesResolver.getTemplates("templates"));
        assertEquals(3, templatesResolver.getTemplates("examples").length);
        assertEquals(7, templatesResolver.getTemplates("tutorials").length);
    }

    @Test
    public void testGetProjectFiles() throws Exception {
        PredefinedTemplatesResolver templatesResolver = new PredefinedTemplatesResolver();
        ProjectFile[] projectFiles = templatesResolver.getProjectFiles("examples", "Example 3 - Auto Policy Calculation");
        assertEquals(3, projectFiles.length);
        assertTrue(contains(projectFiles, "AutoPolicyCalculation.xls"));
        assertTrue(contains(projectFiles, "AutoPolicyTests.xls"));
        assertTrue(contains(projectFiles, "UServ Auto Insurance Case Study.doc"));

        close(projectFiles);
    }
}
