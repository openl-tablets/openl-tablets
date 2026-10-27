package org.openl.rules.project.resolving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;

class ProjectResolverTest {

    @Test
    void testStrategySelection() {
        ProjectResolver resolver = ProjectResolver.getInstance();
        assertInstanceOf(ProjectDescriptorBasedResolvingStrategy.class, resolver.isRulesProject(Path.of("test-resources/descriptor")));
        assertInstanceOf(SimpleXlsResolvingStrategy.class, resolver.isRulesProject(Path.of("test-resources/excel")));
    }

    /** A build copies the workbooks into its output folder, which a module path with a wildcard matches too. */
    @Test
    void findsNoModuleInAnIgnoredFolder(@TempDir Path project) throws Exception {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Rating</name>
                    <modules>
                        <module>
                            <rules-root path="**/*.xlsx"/>
                        </module>
                    </modules>
                </project>""");
        write(project.resolve("rules/Rating.xlsx"));
        write(project.resolve("target/classes/rules/Rating.xlsx"));
        var resolver = ProjectResolver.getInstance();

        var all = resolver.resolve(project);
        var kept = resolver.resolve(project, file -> file.equals(project.resolve("target")));

        assertEquals("rules/Rating.xlsx, target/classes/rules/Rating.xlsx", pathsOf(all.getModules()));
        assertEquals("rules/Rating.xlsx", pathsOf(kept.getModules()));
    }

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "workbook");
    }

    private static String pathsOf(List<Module> modules) {
        return modules.stream().map(Module::getRulesRootPath).collect(Collectors.joining(", "));
    }
}
