package org.openl.studio.repositories.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.workspace.filter.PathFilter;

/**
 * The paths the repositories neither show nor change, as the {@link IgnoredPaths#PROPERTY} setting lists them.
 */
@Configuration
public class IgnoredPathsConfiguration {

    @Bean
    public IgnoredPaths ignoredPaths(@Value("${" + IgnoredPaths.PROPERTY + ":}") String setting) {
        return IgnoredPaths.of(setting);
    }

    /**
     * Leaves the ignored paths out of a project uploaded as a ZIP archive.
     */
    @Bean
    public PathFilter zipFilter(IgnoredPaths ignoredPaths) {
        return path -> !ignoredPaths.matches(path);
    }
}
