package org.openl.rules.webstudio.web.repository.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

/**
 * @author nsamatov, Yury Molchan.
 */
public class PredefinedTemplatesResolverTest extends TemplatesResolverTest {
    @Test
    public void testGetCategories() throws Exception {
        Collection<String> categories = new PredefinedTemplatesResolver().getCategories();
        assertEquals(3, categories.size());
        assertTrue(categories.containsAll(Arrays.asList("templates", "examples", "tutorials")));
    }

    @Test
    public void testGetTemplates() throws Exception {
        PredefinedTemplatesResolver templatesResolver = new PredefinedTemplatesResolver();

        Collection<String> templates = templatesResolver.getTemplates("templates");
        assertEquals(2, templates.size());
        assertTrue(templates.containsAll(Arrays.asList("Empty Project", "Sample Project")));
        assertEquals(3, templatesResolver.getTemplates("examples").size());
        assertEquals(8, templatesResolver.getTemplates("tutorials").size());
    }

    @Test
    public void testGetProjectFiles() throws Exception {
        PredefinedTemplatesResolver templatesResolver = new PredefinedTemplatesResolver();
        ProjectFile[] projectFiles = templatesResolver.getProjectFiles("examples",
            "Example 3 - Auto Policy Calculation");
        assertEquals(3, projectFiles.length);
        assertTrue(contains(projectFiles, "AutoPolicyCalculation.xlsx"));
        assertTrue(contains(projectFiles, "AutoPolicyTests.xlsx"));
        assertTrue(contains(projectFiles, "UServ Auto Insurance Case Study.doc"));

        close(projectFiles);
    }
}
