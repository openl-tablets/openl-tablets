package org.openl.rules.webstudio.web.repository.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;

/**
 * @author nsamatov, Yury Molchan.
 */
public class CustomTemplatesResolverTest extends TemplatesResolverTest {
    private static final String CUSTOM_TEMPLATES_CATEGORY = "Custom templates";
    private static final String RATING_TEMPLATES_CATEGORY = "Rating Templates";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String webStudioHomePath;

    @Before
    public void setUp() throws Exception {
        webStudioHomePath = tempFolder.getRoot().getPath();
        File webStudioHome = tempFolder.newFolder(CustomTemplatesResolver.PROJECT_TEMPLATES_FOLDER);

        // Create project templates
        File sample1Folder = createFolder(new File(webStudioHome, CUSTOM_TEMPLATES_CATEGORY), "Sample1 project");
        File sample2Folder = createFolder(new File(webStudioHome, CUSTOM_TEMPLATES_CATEGORY), "Sample2 project");
        File autoRatingFolder = createFolder(new File(webStudioHome, RATING_TEMPLATES_CATEGORY), "Auto rating");

        // Create project templates content
        touch(new File(sample1Folder, "Main1.xls"));
        touch(new File(sample2Folder, "Main2.xlsx"));

        // Auto rating project
        touch(new File(autoRatingFolder, ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME));
        File rulesFolder = createFolder(autoRatingFolder, "rules");
        touch(new File(rulesFolder, "Rating.xlsx"));
    }

    @Test
    public void testGetCategories() {
        Collection<String> categories = new CustomTemplatesResolver(webStudioHomePath).getCategories();
        assertEquals(2, categories.size());
        assertTrue(categories.containsAll(Arrays.asList(CUSTOM_TEMPLATES_CATEGORY, RATING_TEMPLATES_CATEGORY)));

        String absentWebStudioHome = tempFolder.getRoot().getPath() + "/not-exist";
        Collection<String> categories2 = new CustomTemplatesResolver(absentWebStudioHome).getCategories();
        assertTrue(categories2.isEmpty());
    }

    @Test
    public void testGetTemplates() {
        CustomTemplatesResolver templatesResolver = new CustomTemplatesResolver(webStudioHomePath);

        Collection<String> templates1 = templatesResolver.getTemplates(CUSTOM_TEMPLATES_CATEGORY);
        assertEquals(2, templates1.size());
        assertTrue(templates1.containsAll(Arrays.asList("Sample1 project", "Sample2 project")));

        Collection<String> templates2 = templatesResolver.getTemplates(RATING_TEMPLATES_CATEGORY);
        assertEquals(1, templates2.size());
        assertTrue(templates2.contains("Auto rating"));
    }

    @Test
    public void testGetProjectFiles() {
        CustomTemplatesResolver templatesResolver = new CustomTemplatesResolver(webStudioHomePath);
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
        assertTrue(contains(projectFiles, ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME));
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
