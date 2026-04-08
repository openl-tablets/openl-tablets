package org.openl.rules.webstudio.web.repository.project;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;

/**
 * @author nsamatov.
 */
@Slf4j
public class PredefinedTemplatesResolver extends TemplatesResolver {

    private static final String TEMPLATES_PATH = "org.openl.rules.demo.";
    private static final List<String> PREDEFINED_CATEGORIES = Arrays.asList("templates", "examples", "tutorials");

    @Override
    protected List<String> resolveCategories() {
        return PREDEFINED_CATEGORIES;
    }

    @Override
    protected List<String> resolveTemplates(String category) {
        List<String> templateNames = new ArrayList<>();

        try {
            for (Resource resource : getFolderResources(TEMPLATES_PATH + category + "/*")) {
                if (!ResourceUtils.isFileURL(resource.getURL())) {
                    // JAR file
                    String templateUrl = URLDecoder.decode(resource.getURL().getPath(), "UTF8");
                    String[] templateParsed = templateUrl.split("/");
                    templateNames.add(templateParsed[templateParsed.length - 1]);
                } else {
                    // File System
                    templateNames.add(resource.getFilename());
                }
            }

        } catch (Exception e) {
            log.error("Failed to get project templates", e);
        }
        Collections.sort(templateNames);
        return templateNames;
    }

    @Override
    public ProjectFile[] getProjectFiles(String category, String templateName) {
        String url = TEMPLATES_PATH + category + "/" + templateName;

        List<ProjectFile> templateFiles = new ArrayList<>();
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] templates = resourceResolver.getResources(url + "/*");
            for (Resource resource : templates) {
                templateFiles.add(new ProjectFile(resource.getFilename(), resource.getInputStream()));
            }
        } catch (Exception e) {
            log.error("Failed to get project template: {}", url, e);
        }

        return templateFiles.isEmpty() ? new ProjectFile[0]
                : templateFiles.toArray(new ProjectFile[0]);
    }

    private Resource[] getFolderResources(String folderPattern) throws IOException {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        // JAR file
        Resource[] resources = resourceResolver.getResources(folderPattern + "/");
        if (resources.length == 0) {
            // File System
            resources = resourceResolver.getResources(folderPattern);
        }
        return resources;
    }

}
