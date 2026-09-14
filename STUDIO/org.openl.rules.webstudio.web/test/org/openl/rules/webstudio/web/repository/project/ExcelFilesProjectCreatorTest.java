package org.openl.rules.webstudio.web.repository.project;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.upload.RulesProjectBuilder;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;

class ExcelFilesProjectCreatorTest {

    @Test
    void createsDefaultLayoutForUploadedXlsxFiles() throws Exception {
        var pricing = projectFile("Pricing.xlsx", "pricing");
        var costs = projectFile("Costs.xlsx", "costs");
        var readme = projectFile("README.md", "readme");
        var creator = creator(pricing, costs, readme);

        try (var builders = mockConstruction(RulesProjectBuilder.class)) {
            var builder = creator.getProjectBuilder();

            assertSame(builders.constructed().getFirst(), builder);
            var names = ArgumentCaptor.forClass(String.class);
            var content = ArgumentCaptor.forClass(InputStream.class);
            verify(builder, times(4)).addFile(names.capture(), content.capture());
            assertEquals(List.of("rules.xml", "rules/Pricing.xlsx", "rules/Costs.xlsx", "README.md"),
                    names.getAllValues());
            assertEquals("<project/>\n",
                    new String(content.getAllValues().getFirst().readAllBytes(), StandardCharsets.UTF_8));
            assertArrayEquals("pricing".getBytes(StandardCharsets.UTF_8),
                    content.getAllValues().get(1).readAllBytes());
            assertArrayEquals("costs".getBytes(StandardCharsets.UTF_8),
                    content.getAllValues().get(2).readAllBytes());
            assertArrayEquals("readme".getBytes(StandardCharsets.UTF_8),
                    content.getAllValues().get(3).readAllBytes());
        } finally {
            creator.destroy();
        }
    }

    @Test
    void declaresLegacyModulesAndRetainsDefaultXlsxPatterns() throws Exception {
        var xls = projectFile("Legacy.xls", "legacy");
        var xlsm = projectFile("Macros.xlsm", "macros");
        var xlsx = projectFile("Current.xlsx", "current");
        var creator = creator(xls, xlsm, xlsx);

        try (var builders = mockConstruction(RulesProjectBuilder.class)) {
            var builder = creator.getProjectBuilder();
            var names = ArgumentCaptor.forClass(String.class);
            var content = ArgumentCaptor.forClass(InputStream.class);
            verify(builder, times(4)).addFile(names.capture(), content.capture());

            var descriptor = ProjectDescriptor.read(content.getAllValues().getFirst());
            assertNotNull(descriptor);
            assertEquals(List.of("rules/**/*.xlsx", "tests/**/*.xlsx", "rules/Legacy.xls", "rules/Macros.xlsm"),
                    descriptor.getModules().stream().map(Module::getRulesRootPath).toList());
        } finally {
            creator.destroy();
        }
    }

    @Test
    void preservesTemplateLayout() throws Exception {
        var rules = projectFile("rules/Main.xlsx", "rules");
        var descriptor = projectFile("rules.xml", "<project/>");
        var creator = creator(rules, descriptor);

        try (var builders = mockConstruction(RulesProjectBuilder.class)) {
            var builder = creator.getProjectBuilder();
            var names = ArgumentCaptor.forClass(String.class);
            verify(builder, times(2)).addFile(names.capture(), any(InputStream.class));

            assertEquals(List.of("rules/Main.xlsx", "rules.xml"), names.getAllValues());
        } finally {
            creator.destroy();
        }
    }

    @Test
    void keepsMacOSResourceForksOutsideRuleModules() throws Exception {
        var workbook = projectFile("Main.xlsx", "workbook");
        var resourceFork = projectFile("__MACOSX/._Main.xlsx", "metadata");
        var creator = creator(workbook, resourceFork);

        try (var builders = mockConstruction(RulesProjectBuilder.class)) {
            var builder = creator.getProjectBuilder();
            var names = ArgumentCaptor.forClass(String.class);
            verify(builder, times(3)).addFile(names.capture(), any(InputStream.class));

            assertEquals(List.of("rules.xml", "rules/Main.xlsx", "__MACOSX/._Main.xlsx"), names.getAllValues());
        } finally {
            creator.destroy();
        }
    }

    private static ExcelFilesProjectCreator creator(ProjectFile... files) {
        var pathFilter = mock(PathFilter.class);
        when(pathFilter.accept(anyString())).thenReturn(true);
        return new ExcelFilesProjectCreator(mock(Repository.class),
                "Project",
                "",
                mock(UserWorkspace.class),
                "Create Project",
                pathFilter,
                Map.of(),
                files);
    }

    private static ProjectFile projectFile(String name, String content) {
        return new ProjectFile(name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
