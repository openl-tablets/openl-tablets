package org.openl.studio.repositories.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.workspace.filter.PathFilter;

class IgnoredPathsConfigurationTest {

    @Test
    void readsTheSettingsAndLeavesTheIgnoredAndProtectedPathsOutOfAnUpload() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("repository.default-ignored-paths", "**/.DS_Store",
                            IgnoredPaths.PROPERTY, "${repository.default-ignored-paths}, **/target/**",
                            ProjectFiles.PROTECTED_PROPERTY, "pom.xml",
                            ProjectFiles.HIDDEN_PROPERTY, "rules.xml")));
            context.register(IgnoredPathsConfiguration.class);
            context.refresh();

            var ignoredPaths = context.getBean(IgnoredPaths.class);
            assertTrue(ignoredPaths.matches("p/.DS_Store"));
            assertTrue(ignoredPaths.matches("p/target/Rules.xlsx"));
            var projectFiles = context.getBean(ProjectFiles.class);
            assertTrue(projectFiles.isProtected("pom.xml"));
            assertTrue(projectFiles.isHidden("rules.xml"));
            var zipFilter = context.getBean("zipFilter", PathFilter.class);
            assertFalse(zipFilter.accept("p/target/Rules.xlsx"));
            // A ZIP archive opened as a file system names its entries from a slash.
            assertFalse(zipFilter.accept("/.DS_Store"));
            assertFalse(zipFilter.accept("pom.xml"));
            assertFalse(zipFilter.accept("/pom.xml"));
            assertTrue(zipFilter.accept("rules/pom.xml"));
            assertTrue(zipFilter.accept("rules.xml"));
        }
    }

    @Test
    void ignoresTheGitFolderAloneWithoutSetting() {
        try (var context = new AnnotationConfigApplicationContext(IgnoredPathsConfiguration.class)) {
            var ignoredPaths = context.getBean(IgnoredPaths.class);
            assertTrue(ignoredPaths.matches(".git/config"));
            assertFalse(ignoredPaths.matches("p/pom.xml"));
            var projectFiles = context.getBean(ProjectFiles.class);
            assertFalse(projectFiles.isProtected("pom.xml"));
            assertFalse(projectFiles.isHidden("rules.xml"));
        }
    }
}
