package org.openl.studio.repositories.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.workspace.filter.PathFilter;

class IgnoredPathsConfigurationTest {

    @Test
    void readsTheSettingAndLeavesTheIgnoredPathsOutOfAnUpload() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("repository.default-ignored-paths", "**/pom.xml",
                            IgnoredPaths.PROPERTY, "${repository.default-ignored-paths}, **/target/**")));
            context.register(IgnoredPathsConfiguration.class);
            context.refresh();

            var ignoredPaths = context.getBean(IgnoredPaths.class);
            assertTrue(ignoredPaths.matches("p/pom.xml"));
            assertTrue(ignoredPaths.matches("p/target/Rules.xlsx"));
            var zipFilter = context.getBean("zipFilter", PathFilter.class);
            assertFalse(zipFilter.accept("p/target/Rules.xlsx"));
            assertTrue(zipFilter.accept("p/rules.xml"));
        }
    }

    @Test
    void ignoresTheGitFolderAloneWithoutSetting() {
        try (var context = new AnnotationConfigApplicationContext(IgnoredPathsConfiguration.class)) {
            var ignoredPaths = context.getBean(IgnoredPaths.class);
            assertTrue(ignoredPaths.matches(".git/config"));
            assertFalse(ignoredPaths.matches("p/pom.xml"));
        }
    }
}
