package org.openl.rules.webstudio.web.repository.project;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author nsamatov.
 */
public class CustomTemplatesResolverTest extends TemplatesResolverTest {
    private static final String CUSTOM_TEMPLATES_CATEGORY = "Custom templates";
    private static final String RATING_TEMPLATES_CATEGORY = "Rating Templates";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        System.setProperty("webstudio.home", tempFolder.getRoot().getPath());

        File webStudioHome = tempFolder.newFolder(CustomTemplatesResolver.PROJECT_TEMPLATES_FOLDER);

        // Create project templates
        File sample1Folder = createFolder(new File(webStudioHome, CUSTOM_TEMPLATES_CATEGORY), "Sample1 project");
        File sample2Folder = createFolder(new File(webStudioHome, CUSTOM_TEMPLATES_CATEGORY), "Sample2 project");
        File autoRatingFolder = createFolder(new File(webStudioHome, RATING_TEMPLATES_CATEGORY), "Auto rating");

        // Create project templates content
        touch(new File(sample1Folder, "Main1.xls"));
        touch(new File(sample2Folder, "Main2.xlsx"));

        // Auto rating project
        touch(new File(autoRatingFolder, "rules.xml"));
        File rulesFolder = createFolder(autoRatingFolder, "rules");
        touch(new File(rulesFolder, "Rating.xlsx"));
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty("webstudio.home");
    }

    @Test
    public void testGetCategories() throws Exception {
        assertArrayEquals(new String[]{CUSTOM_TEMPLATES_CATEGORY, RATING_TEMPLATES_CATEGORY}, new CustomTemplatesResolver().getCategories());

        System.setProperty("webstudio.home", tempFolder.getRoot().getPath() + "/not-exist");
        assertArrayEquals(new String[]{}, new CustomTemplatesResolver().getCategories());
        System.clearProperty("webstudio.home");
    }

    @Test
    public void testGetTemplates() throws Exception {
        CustomTemplatesResolver templatesResolver = new CustomTemplatesResolver();

        assertArrayEquals(new String[]{"Sample1 project", "Sample2 project"}, templatesResolver.getTemplates(CUSTOM_TEMPLATES_CATEGORY));
        assertArrayEquals(new String[]{"Auto rating"}, templatesResolver.getTemplates(RATING_TEMPLATES_CATEGORY));
    }

    @Test
    public void testGetProjectFiles() throws Exception {
        CustomTemplatesResolver templatesResolver = new CustomTemplatesResolver();
        ProjectFile[] projectFiles = templatesResolver.getProjectFiles(CUSTOM_TEMPLATES_CATEGORY, "Sample1 project");
        assertEquals(1, projectFiles.length);
        assertTrue(contains(projectFiles, "Main1.xls"));
        close(projectFiles);

        projectFiles = templatesResolver.getProjectFiles(CUSTOM_TEMPLATES_CATEGORY, "Sample2 project");
        assertEquals(1, projectFiles.length);
        assertTrue(contains(projectFiles, "Main2.xlsx"));
        close(projectFiles);

        projectFiles = templatesResolver.getProjectFiles(RATING_TEMPLATES_CATEGORY, "Auto rating");
        assertEquals(2, projectFiles.length);
        assertTrue(contains(projectFiles, "rules.xml"));
        assertTrue(contains(projectFiles, "rules/Rating.xlsx"));
        close(projectFiles);
    }

    private File createFolder(File parentFolder, String subFolder) {
        File folder = new File(parentFolder, subFolder);
        if (!folder.mkdirs()) {
            throw new IllegalStateException();
        }
        return folder;
    }

    private void touch(File file) throws IOException {
        if (!file.createNewFile()) {
            throw new IllegalStateException();
        }
    }
}
