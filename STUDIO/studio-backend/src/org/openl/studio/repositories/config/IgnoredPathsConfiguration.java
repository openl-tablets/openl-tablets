package org.openl.studio.repositories.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.workspace.filter.PathFilter;

/**
 * The files of the repositories and the projects kept apart from the rules: the paths the repositories neither show
 * nor change, as the {@link IgnoredPaths#PROPERTY} setting lists them, and the protected and hidden files of every
 * project, as the {@link ProjectFiles#PROTECTED_PROPERTY} and {@link ProjectFiles#HIDDEN_PROPERTY} settings list them.
 */
@Configuration
public class IgnoredPathsConfiguration {

    @Bean
    public IgnoredPaths ignoredPaths(@Value("${" + IgnoredPaths.PROPERTY + ":}") String setting) {
        return IgnoredPaths.of(setting);
    }

    @Bean
    public ProjectFiles projectFiles(@Value("${" + ProjectFiles.PROTECTED_PROPERTY + ":}") String protectedFiles,
                                     @Value("${" + ProjectFiles.HIDDEN_PROPERTY + ":}") String hiddenFiles) {
        return ProjectFiles.of(protectedFiles, hiddenFiles);
    }

    /**
     * Leaves out of a project uploaded as a ZIP archive the ignored paths and the protected files, by their paths
     * relative to the root of the project.
     */
    @Bean
    public PathFilter zipFilter(IgnoredPaths ignoredPaths, ProjectFiles projectFiles) {
        return path -> !projectFiles.isProtected(path) && !ignoredPaths.matches(path);
    }
}
