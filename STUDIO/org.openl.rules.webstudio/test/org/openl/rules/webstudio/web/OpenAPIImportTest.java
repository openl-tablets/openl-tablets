package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * Runs the Editor's "Import from OpenAPI" (Tables generation) against a project on disk, the way the other OpenAPI
 * suites run generation and validation: every folder under {@code test-resources/openapi-import} is a project as it
 * stands before the import, and {@code expected-rules.xml} beside it is the descriptor the import must leave behind.
 *
 * <p>A case folder holds the project's {@code rules.xml}, its {@code openapi.json}, and the expected descriptor. The
 * module names and paths the dialog asks for are fixed: the algorithms module is {@code Algorithms}
 * ({@code rules/Algorithms.xlsx}) and the data module is {@code Models} ({@code rules/Models.xlsx}), so a case is
 * described entirely by what its rules.xml declares beforehand.
 */
class OpenAPIImportTest {

    private static final String CASES = "test-resources/openapi-import";
    private static final String ALGORITHMS_MODULE = "Algorithms";
    private static final String MODELS_MODULE = "Models";
    private static final String OPENAPI_FILE = "openapi.json";

    static Stream<String> cases() throws IOException {
        try (var folders = Files.list(Path.of(CASES))) {
            return folders.filter(Files::isDirectory).map(folder -> folder.getFileName().toString()).sorted().toList()
                    .stream();
        }
    }

    @ParameterizedTest
    @MethodSource("cases")
    void importGeneratesModulesIntoTheDescriptor(String name, @TempDir Path workspace) throws Exception {
        var project = copyCase(name, workspace);

        runImport(project);

        assertEquals(read(Path.of(CASES, name, "expected-rules.xml")),
                read(project.resolve(ProjectDescriptor.FILE_NAME)),
                "The descriptor the import left behind differs from the expected one.");
    }

    /** Copies the case files the import reads, folders and all, leaving out the expected descriptor. */
    private static Path copyCase(String name, Path workspace) throws IOException {
        var source = Path.of(CASES, name);
        var expected = fileCount(source);
        var project = workspace.resolve(name);
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(f -> !f.getFileName().toString().startsWith("expected-")).toList()) {
                var target = project.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                }
            }
        }
        assertEquals(expected, fileCount(project), "The case must arrive in the workspace with all its files.");
        return project;
    }

    private static long fileCount(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(f -> !f.getFileName().toString().startsWith("expected-"))
                    .count();
        }
    }

    /**
     * Drives {@link ProjectBean#regenerateOpenAPI()} the way the JSF dialog does.
     *
     * <p>The project is answered from the folder on disk: reading the declared descriptor and writing the generated
     * files both go through it, so the descriptor left behind is the one the flow really produced.
     */
    private static void runImport(Path project) throws Exception {
        // A project without rules.xml is a supported state: the resolver answers with a descriptor of its own.
        var declared = ProjectDescriptor.read(project);
        var descriptor = declared != null ? declared : new ProjectDescriptor();
        if (declared == null) {
            descriptor.setName(project.getFileName().toString());
            descriptor.setModules(new ArrayList<>());
        }
        descriptor.setProjectFolder(project);

        var rulesProject = mock(RulesProject.class, RETURNS_DEEP_STUBS);
        when(rulesProject.tryLock()).thenReturn(true);
        // A real FileData: the permission checks read the repository path off it.
        var fileData = new FileData();
        fileData.setName(project.getFileName().toString());
        lenient().when(rulesProject.getFileData()).thenReturn(fileData);
        lenient().when(rulesProject.getName()).thenReturn(descriptor.getName());
        lenient().when(rulesProject.hasArtefact(anyString()))
                .thenAnswer(call -> Files.exists(project.resolve(call.<String>getArgument(0))));
        lenient().when(rulesProject.addResource(anyString(), any(InputStream.class)))
                .thenAnswer(call -> writeArtefact(project, call.getArgument(0), call.getArgument(1)));
        lenient().when(rulesProject.getArtefact(anyString()))
                .thenAnswer(call -> artefact(project, call.getArgument(0)));
        // The spec is read from the workspace by its project-relative path, the way the Editor hands it over.
        var openApiFile = mock(AProjectResource.class, RETURNS_DEEP_STUBS);
        lenient().when(openApiFile.getArtefactPath().getStringValue())
                .thenReturn(project.getFileName() + "/" + OPENAPI_FILE);
        lenient().when(rulesProject.getArtefactByPath(any())).thenReturn(openApiFile);

        var studio = mock(WebStudio.class, RETURNS_DEEP_STUBS);
        when(studio.getCurrentProjectDescriptor()).thenReturn(descriptor);
        when(studio.getCurrentProject()).thenReturn(rulesProject);
        when(studio.getWorkspacePath()).thenReturn(project.getParent().toString());
        lenient().when(studio.resolveProject(descriptor)).thenReturn(descriptor);
        // The dialog asks for module names the project does not have yet, so the import generates both.
        lenient().when(studio.getModule(any(), nullable(String.class))).thenReturn(null);

        var acl = mock(RepositoryAclService.class);
        lenient().when(acl.isGranted(any(), anyList())).thenReturn(true);
        lenient().when(acl.isGranted(any(), anyBoolean(), any())).thenReturn(true);
        lenient().when(acl.isGranted(nullable(String.class), nullable(String.class), anyList())).thenReturn(true);
        lenient().when(acl.getPath(any(RulesProject.class))).thenReturn(descriptor.getName());
        lenient().when(acl.hasAcl(any())).thenReturn(true);

        try (var utils = mockStatic(WebStudioUtils.class)) {
            utils.when(WebStudioUtils::getWebStudio).thenReturn(studio);
            parameter(utils, "openAPIPath", OPENAPI_FILE);
            parameter(utils, "algorithmModuleName", ALGORITHMS_MODULE);
            parameter(utils, "modelModuleName", MODELS_MODULE);
            parameter(utils, "algorithmModulePath", "rules/" + ALGORITHMS_MODULE + ".xlsx");
            parameter(utils, "modelModulePath", "rules/" + MODELS_MODULE + ".xlsx");

            new ProjectBean(acl).regenerateOpenAPI();
        }

        assertTrue(Files.exists(project.resolve("rules/" + MODELS_MODULE + ".xlsx")),
                "The import must write the data module it generated.");
    }

    private static void parameter(org.mockito.MockedStatic<WebStudioUtils> utils, String name, String value) {
        utils.when(() -> WebStudioUtils.getRequestParameter("generateOpenAPIForm:" + name)).thenReturn(value);
    }

    private static AProjectResource writeArtefact(Path project, String name, InputStream content) throws Exception {
        var file = project.resolve(name);
        Files.createDirectories(file.getParent());
        try (content) {
            Files.copy(content, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return artefact(project, name);
    }

    /** An artefact whose content is the file itself, so a write through it lands in the project folder. */
    private static AProjectResource artefact(Path project, String name) throws Exception {
        var artefact = mock(AProjectResource.class);
        var file = project.resolve(name);
        var data = new FileData();
        data.setName(name);
        lenient().when(artefact.getFileData()).thenReturn(data);
        lenient().when(artefact.getContent()).thenAnswer(call -> Files.newInputStream(file));
        lenient().doAnswer(call -> writeArtefact(project, name, call.getArgument(0)))
                .when(artefact)
                .setContent(any(InputStream.class));
        return artefact;
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file).replace("\r\n", "\n").strip();
    }

}
