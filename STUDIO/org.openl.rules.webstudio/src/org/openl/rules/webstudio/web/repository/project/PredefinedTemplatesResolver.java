package org.openl.rules.webstudio.web.repository.project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.springframework.web.util.UriUtils;

/**
 * @author nsamatov.
 */
@Slf4j
public class PredefinedTemplatesResolver extends TemplatesResolver {

    private static final String TEMPLATES_PATH = "org.openl.rules.demo.";
    private static final String TEMPLATES_CATEGORY = "templates";
    private static final String EXAMPLES_CATEGORY = "examples";
    private static final String TUTORIALS_CATEGORY = "tutorials";
    private static final List<String> PREDEFINED_CATEGORIES = List.of(TEMPLATES_CATEGORY,
            EXAMPLES_CATEGORY,
            TUTORIALS_CATEGORY);
    private final ResourcePatternResolver resourceResolver;

    public PredefinedTemplatesResolver() {
        this(new PathMatchingResourcePatternResolver());
    }

    PredefinedTemplatesResolver(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    protected List<String> resolveCategories() {
        return PREDEFINED_CATEGORIES;
    }

    @Override
    protected List<String> resolveTemplates(String category) {
        var categoryPath = resolveCategoryPath(category);
        if (categoryPath.isEmpty()) {
            return List.of();
        }

        var templateNames = new ArrayList<String>();

        try {
            for (Resource resource : getFolderResources(categoryPath.get() + "/*")) {
                if (!ResourceUtils.isFileURL(resource.getURL())) {
                    // JAR file
                    var templateUrl = UriUtils.decode(resource.getURL().getPath(), StandardCharsets.UTF_8);
                    var templateParsed = templateUrl.split("/");
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
        var templateRoot = resolveTemplateRoot(category, templateName);
        if (templateRoot.isEmpty()) {
            return new ProjectFile[0];
        }

        var templateFiles = new ArrayList<ProjectFile>();
        try {
            var templates = resourceResolver.getResources(templateRoot.get() + "**/*");
            for (Resource resource : templates) {
                if (!isDirectory(resource)) {
                    templateFiles.add(new ProjectFile(relativePath(resource, templateRoot.get()),
                        resource.getInputStream()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to get predefined project template", e);
        }

        return templateFiles.isEmpty() ? new ProjectFile[0]
                : templateFiles.toArray(new ProjectFile[0]);
    }

    private Optional<String> resolveTemplateRoot(String category, String templateName) {
        var categoryPath = resolveCategoryPath(category);
        if (categoryPath.isEmpty()) {
            return Optional.empty();
        }

        var templateNames = resolveTemplates(category);
        var templateIndex = templateNames.indexOf(templateName);
        if (templateIndex < 0) {
            return Optional.empty();
        }

        var categoryRoot = Path.of(categoryPath.get()).normalize();
        var templatePath = categoryRoot.resolve(templateNames.get(templateIndex)).normalize();
        if (!templatePath.startsWith(categoryRoot)
                || !categoryRoot.equals(templatePath.getParent())) {
            return Optional.empty();
        }

        return Optional.of(templatePath.toString().replace(File.separatorChar, '/') + "/");
    }

    private static Optional<String> resolveCategoryPath(String category) {
        return switch (category) {
            case TEMPLATES_CATEGORY -> Optional.of(TEMPLATES_PATH + TEMPLATES_CATEGORY);
            case EXAMPLES_CATEGORY -> Optional.of(TEMPLATES_PATH + EXAMPLES_CATEGORY);
            case TUTORIALS_CATEGORY -> Optional.of(TEMPLATES_PATH + TUTORIALS_CATEGORY);
            default -> Optional.empty();
        };
    }

    private static boolean isDirectory(Resource resource) throws IOException {
        var resourceUrl = resource.getURL();
        return resourceUrl.getPath().endsWith("/")
                || ResourceUtils.isFileURL(resourceUrl) && resource.getFile().isDirectory();
    }

    private static String relativePath(Resource resource, String templateRoot) throws IOException {
        var resourcePath = UriUtils.decode(resource.getURL().getPath(), StandardCharsets.UTF_8);
        return resourcePath.substring(resourcePath.lastIndexOf(templateRoot) + templateRoot.length());
    }

    private Resource[] getFolderResources(String folderPattern) throws IOException {
        // JAR file
        var resources = resourceResolver.getResources(folderPattern + "/");
        if (resources.length == 0) {
            // File System
            resources = resourceResolver.getResources(folderPattern);
        }
        return resources;
    }

}
