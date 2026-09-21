package org.openl.rules.webstudio.web.repository.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author nsamatov.
 */
@Slf4j
public class CustomTemplatesResolver extends TemplatesResolver {
    public static final String PROJECT_TEMPLATES_FOLDER = "project-templates";

    /** The template folder itself, where the walk over its files starts. */
    private static final String ROOT_FOLDER = "";

    private final Path templatesPath;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(
            new TemplateResourceLoader());

    public CustomTemplatesResolver(String webStudioHome) {
        templatesPath = Path.of(webStudioHome, PROJECT_TEMPLATES_FOLDER).toAbsolutePath().normalize();
    }

    @Override
    protected List<String> resolveCategories() {
        return getFolders(templatesPath);
    }

    @Override
    protected List<String> resolveTemplates(String category) {
        return categoryFolder(category).map(this::getFolders).orElseGet(List::of);
    }

    @Override
    public ProjectFile[] getProjectFiles(String category, String templateName) {
        var templateFolder = categoryFolder(category)
                .flatMap(folder -> subFolder(folder, templateName, getTemplates(category)));
        if (templateFolder.isEmpty()) {
            return new ProjectFile[0];
        }

        var templateFiles = getProjectFilesRecursively(asLocation(templateFolder.get()), ROOT_FOLDER);

        return templateFiles.toArray(new ProjectFile[0]);
    }

    private Optional<Path> categoryFolder(String category) {
        return subFolder(templatesPath, category, getCategories());
    }

    /**
     * Resolves a subfolder a user has asked for by name.
     *
     * <p>The name must be one of the folders that are really there, and must name a folder directly under the
     * parent one. Anything else resolves to nothing, so that a request cannot reach outside of the templates
     * folder or turn into a resource pattern of its own.
     */
    private static Optional<Path> subFolder(Path parent, String name, List<String> knownNames) {
        if (!knownNames.contains(name)) {
            return Optional.empty();
        }
        var folder = parent.resolve(name).normalize();
        return parent.equals(folder.getParent()) ? Optional.of(folder) : Optional.empty();
    }

    private List<ProjectFile> getProjectFilesRecursively(String baseUrl, final String folder) {
        var templateFiles = new ArrayList<ProjectFile>();

        try {
            String locationPattern = folder.isEmpty() ? (baseUrl + "/*") : (baseUrl + "/" + folder + "/*");
            var resources = resourcePatternResolver.getResources(locationPattern);
            for (Resource resource : resources) {
                var filename = resource.getFilename();
                String relativePath = folder.isEmpty() ? filename : (folder + "/" + filename);
                if (resource.getFile().isDirectory()) {
                    templateFiles.addAll(getProjectFilesRecursively(baseUrl, relativePath));
                } else {
                    templateFiles.add(new ProjectFile(relativePath, resource.getInputStream()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to get project template", e);
        }
        return templateFiles;
    }

    private List<String> getFolders(Path parent) {
        var folderNames = new ArrayList<String>();
        try {
            for (Resource folder : resourcePatternResolver.getResources(asLocation(parent) + "/*")) {
                if (folder.getFile().isDirectory()) {
                    folderNames.add(folder.getFilename());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return folderNames;
    }

    /** The folder as a resource location: the resolver reads '/' as a separator on every platform. */
    private static String asLocation(Path folder) {
        return folder.toString().replace(File.separatorChar, '/');
    }

    private static class TemplateResourceLoader implements ResourceLoader {
        @Override
        public Resource getResource(String location) {
            return new FileSystemResource(location);
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }
}
