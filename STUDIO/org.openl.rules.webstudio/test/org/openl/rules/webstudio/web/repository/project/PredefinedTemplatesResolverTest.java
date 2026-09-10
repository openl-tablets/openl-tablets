package org.openl.rules.webstudio.web.repository.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * @author nsamatov, Yury Molchan.
 */
class PredefinedTemplatesResolverTest extends TemplatesResolverTest {
    @Test
    void testGetCategories() throws Exception {
        Collection<String> categories = new PredefinedTemplatesResolver().getCategories();
        assertEquals(3, categories.size());
        assertTrue(categories.containsAll(Arrays.asList("templates", "examples", "tutorials")));
    }

    @Test
    void testGetTemplates() throws Exception {
        var templatesResolver = new PredefinedTemplatesResolver();

        var templates = templatesResolver.getTemplates("templates");
        assertEquals(2, templates.size());
        assertTrue(templates.containsAll(Arrays.asList("Empty Project", "Sample Project")));
        assertEquals(3, templatesResolver.getTemplates("examples").size());
        assertEquals(8, templatesResolver.getTemplates("tutorials").size());
    }

    @Test
    void testGetProjectFiles() throws Exception {
        var templatesResolver = new PredefinedTemplatesResolver();
        var projectFiles = templatesResolver.getProjectFiles("examples",
                "Example 3 - Auto Policy Calculation");
        assertEquals(5, projectFiles.length);
        assertTrue(contains(projectFiles, "rules/AutoPolicyCalculation.xlsx"));
        assertTrue(contains(projectFiles, "tests/AutoPolicyTests.xlsx"));
        assertTrue(contains(projectFiles, "UServ Auto Insurance Case Study.doc"));
        assertTrue(contains(projectFiles, "rules.xml"));
        assertTrue(contains(projectFiles, "rules-deploy.xml"));

        close(projectFiles);
    }

    @Test
    void preservesPlusSignsAndDecodesPercentEncodedTemplatePaths() throws Exception {
        var resourceResolver = mock(ResourcePatternResolver.class);
        var templateFolder = mock(Resource.class);
        when(templateFolder.getURL()).thenReturn(URI
                .create("jar:file:/templates.jar!/org.openl.rules.demo.templates/Review+Template/")
                .toURL());
        when(resourceResolver.getResources("org.openl.rules.demo.templates/*/"))
                .thenReturn(new Resource[]{templateFolder});

        var workbook = mock(Resource.class);
        when(workbook.getURL()).thenReturn(URI
                .create("jar:file:/templates.jar!/org.openl.rules.demo.templates/Review+Template/rules/A+B%20C.xlsx")
                .toURL());
        when(workbook.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(resourceResolver.getResources("org.openl.rules.demo.templates/Review+Template/**/*"))
                .thenReturn(new Resource[]{workbook});

        var templatesResolver = new PredefinedTemplatesResolver(resourceResolver);
        assertEquals(List.of("Review+Template"), templatesResolver.getTemplates("templates"));

        var projectFiles = templatesResolver.getProjectFiles("templates", "Review+Template");
        try {
            assertEquals(1, projectFiles.length);
            assertTrue(contains(projectFiles, "rules/A+B C.xlsx"));
        } finally {
            close(projectFiles);
        }
    }

    @Test
    void rejectsUnknownCategoryBeforeResolvingResources() throws Exception {
        var resourceResolver = mock(ResourcePatternResolver.class);
        var templatesResolver = new PredefinedTemplatesResolver(resourceResolver);

        assertEquals(0, templatesResolver.getProjectFiles("../tutorials", "Tutorial 1").length);

        verify(resourceResolver, never()).getResources(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsUnknownTemplateBeforeResolvingProjectFiles() throws Exception {
        var resourceResolver = mock(ResourcePatternResolver.class);
        var templateFolder = mock(Resource.class);
        when(templateFolder.getURL()).thenReturn(URI
                .create("jar:file:/templates.jar!/org.openl.rules.demo.templates/Sample%20Project/")
                .toURL());
        when(resourceResolver.getResources("org.openl.rules.demo.templates/*/"))
                .thenReturn(new Resource[]{templateFolder});

        var templatesResolver = new PredefinedTemplatesResolver(resourceResolver);
        assertEquals(0, templatesResolver.getProjectFiles("templates", "../Sample Project\r\nInjected").length);
        assertEquals(0, templatesResolver.getProjectFiles("templates", "Sample Project\0").length);

        verify(resourceResolver, never()).getResources(org.mockito.ArgumentMatchers.endsWith("**/*"));
    }

    @Test
    void allPredefinedTemplatesUseDefaultProjectLayout() throws Exception {
        var templatesResolver = new PredefinedTemplatesResolver();

        for (var category : templatesResolver.getCategories()) {
            for (var template : templatesResolver.getTemplates(category)) {
                var projectFiles = templatesResolver.getProjectFiles(category, template);
                try {
                    var rulesXml = Arrays.stream(projectFiles)
                            .filter(projectFile -> projectFile.getName().equals("rules.xml"))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(rulesXml, () -> "%s/%s must contain rules.xml".formatted(category, template));
                    var descriptor = ProjectDescriptor.read(rulesXml.getInput());
                    assertNotNull(descriptor);
                    assertTrue(descriptor.getModules().isEmpty(),
                            () -> "%s/%s must use the default modules".formatted(category, template));
                    assertTrue(Arrays.stream(projectFiles)
                            .map(ProjectFile::getName)
                            .filter(fileName -> fileName.endsWith(".xlsx"))
                            .allMatch(fileName -> fileName.startsWith("rules/") || fileName.startsWith("tests/")),
                            () -> "%s/%s must keep workbooks in the standard folders".formatted(category, template));
                } finally {
                    close(projectFiles);
                }
            }
        }
    }
}
