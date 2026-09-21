package org.openl.studio.projects.service.openapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.CompiledOpenClass;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.openapi.OpenApiGenerator;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.openapi.OpenApiSchemaView;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.validation.ValidatedCompiledOpenClass;

/**
 * The OpenAPI specification of a project, written from what the project compiled.
 *
 * <p>What a project publishes is its compiled rules: the methods they expose, the types they take and
 * return. This writes that out as an OpenAPI document and keeps it in the project, so the specification
 * the project ships is the one its rules actually answer to.
 *
 * <p>The document goes where the project already keeps one — the file {@code rules.xml} names, or one of
 * the names the engine reads by default — and the descriptor is left naming it for reconciliation, which
 * is what holds the rules to it from then on.
 *
 * @author Vladyslav Pikus
 */
@Service
@RequiredArgsConstructor
public class ProjectOpenApiService {

    private final WorkspaceProjectService projectService;
    private final ProjectFilesService filesService;
    private final ProjectFileRootFactory fileRootFactory;

    /**
     * Writes the project's own specification, generated from the rules it compiled.
     *
     * <p>The whole project is compiled first: a specification names every rule the project publishes, not
     * only those of the module that happens to be open. A project whose compilation raised errors is
     * refused — a specification written from it would describe rules nobody can call.
     *
     * @param project project to write the specification of
     * @return where the specification was written, and whether it was added or written over
     */
    public OpenApiSchemaView writeSchema(RulesProject project) {
        // Asked of the resolved descriptor before the project is opened: opening one that resolves to no
        // module at all answers "no such project", where what is wrong is that it holds no rules to describe.
        var resolved = projectService.getProjectDescriptor(project);
        if (CollectionUtils.isEmpty(resolved.getModules())) {
            throw new ConflictException("projects.openapi.no-modules.message");
        }
        var model = projectService.openProject(project).awaitCompiled();
        requireCompiled(model.getCompiledOpenClass());

        var root = fileRootFactory.of(project);
        var descriptor = descriptorToWrite(project, root, filesService, () -> resolved);
        var kept = writtenSchemaPath(project, descriptor);
        var path = kept == null ? OpenAPI.Type.JSON.getDefaultFileName() : kept;
        try (var document = generate(model, resolved, typeOf(path))) {
            if (kept == null) {
                filesService.createResource(root, path, document, true);
            } else {
                filesService.updateResource(root, path, document);
            }
        } catch (IOException e) {
            throw new ConflictException("projects.openapi.write-failed.message", path);
        }
        declareReconciliation(project, root, descriptor, path);
        // What the session resolved and compiled was worked out before the descriptor named this file, so it
        // answers for the project as it stood a moment ago.
        projectService.getWebStudio().reset();
        return new OpenApiSchemaView(path, kept == null);
    }

    /**
     * Refuses a project whose compilation raised errors.
     *
     * <p>What the project failed to reconcile against its own specification is not one of them: that is the
     * very thing a specification written afresh answers.
     */
    private static void requireCompiled(CompiledOpenClass compiled) {
        var broken = compiled instanceof ValidatedCompiledOpenClass validated
                ? validated.hasErrors() && !validated.hasOnlyValidationErrors()
                : compiled.hasErrors();
        if (broken) {
            throw new ConflictException("projects.openapi.not-compiled.message");
        }
    }

    /** The document itself, written in the shape its file name asks for. */
    private static InputStream generate(ProjectModel model, ProjectDescriptor resolved, OpenAPI.Type type) {
        try {
            var generator = OpenApiGenerator.builder(resolved, model.getRulesInstantiationStrategy(resolved))
                    .generator();
            var document = generator.generate();
            var written = type == OpenAPI.Type.JSON
                    ? Json.pretty().writeValueAsString(document)
                    : Yaml.pretty().writeValueAsString(document);
            return IOUtils.toInputStream(written);
        } catch (Exception e) {
            throw new ConflictException("projects.openapi.generate-failed.message", messageOf(e));
        }
    }

    private static String messageOf(Exception failure) {
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
    }

    /** What the file name says the document is written in; a name that says nothing is written as JSON. */
    private static OpenAPI.Type typeOf(String path) {
        return Optional.ofNullable(OpenAPI.Type.chooseType(FileUtils.getExtension(path))).orElse(OpenAPI.Type.JSON);
    }

    /**
     * Where the project already keeps a specification: the file the descriptor names, and failing that one of
     * the names the engine reads by default. {@code null} where the project keeps none.
     */
    static @Nullable String writtenSchemaPath(RulesProject project, @Nullable ProjectDescriptor declared) {
        var named = Optional.ofNullable(declared)
                .map(ProjectDescriptor::getOpenapi)
                .map(OpenAPI::getPath)
                .filter(StringUtils::isNotBlank)
                .filter(project::hasArtefact);
        return named.orElseGet(() -> Stream.of(OpenAPI.Type.values())
                .map(OpenAPI.Type::getDefaultFileName)
                .filter(project::hasArtefact)
                .findFirst()
                .orElse(null));
    }

    /**
     * Leaves the descriptor naming the written specification, to be reconciled against from now on.
     *
     * <p>A descriptor that already names it that way is left alone: the write is the specification, and
     * rewriting {@code rules.xml} to say what it already says would only cost the author their formatting.
     */
    private void declareReconciliation(RulesProject project,
                                       FileRoot root,
                                       ProjectDescriptor descriptor,
                                       String path) {
        var openapi = Optional.ofNullable(descriptor.getOpenapi()).orElseGet(OpenAPI::new);
        if (path.equals(openapi.getPath()) && OpenAPI.Mode.RECONCILIATION == openapi.getMode()) {
            return;
        }
        openapi.setPath(path);
        openapi.setMode(OpenAPI.Mode.RECONCILIATION);
        descriptor.setOpenapi(openapi);
        writeDescriptor(project, root, filesService, descriptor);
    }

    /**
     * The descriptor to write back, read from {@code rules.xml} where the project has one.
     *
     * <p>Read from the file rather than taken from the session: the session holds the descriptor the engine
     * resolved, whose modules are the workbooks a wildcard matched. Writing that back would replace what the
     * author declared with the files it happened to stand for.
     *
     * <p>A project with no {@code rules.xml} is given one declaring the modules it resolves to today. A
     * descriptor declaring none is read as the two default patterns, so a project keeping its workbooks
     * anywhere else would lose every module it had the moment a file was written beside them.
     */
    static ProjectDescriptor descriptorToWrite(RulesProject project,
                                               FileRoot root,
                                               ProjectFilesService files,
                                               Supplier<ProjectDescriptor> resolved) {
        if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            var fresh = new ProjectDescriptor();
            fresh.setName(project.getBusinessName());
            fresh.setModules(resolved.get().getModules()
                    .stream()
                    .map(ProjectOpenApiService::declarationOf)
                    .collect(Collectors.toCollection(ArrayList::new)));
            return fresh;
        }
        ProjectDescriptor declared;
        try (var content = files.getResource(root, ProjectDescriptor.FILE_NAME, null).getContent()) {
            declared = ProjectDescriptor.read(content);
        } catch (ProjectException | IOException | RuntimeException e) {
            throw new ConflictException("projects.openapi.descriptor-unreadable.message");
        }
        // A file that cannot be parsed is answered with null rather than with an error, and writing over it
        // would take the project's name, its dependencies and every module it declares with it.
        if (declared == null) {
            throw new ConflictException("projects.openapi.descriptor-unreadable.message");
        }
        return declared;
    }

    /** A module as a descriptor declares one: what it is called, and the workbook it reads. */
    private static Module declarationOf(Module resolved) {
        var module = new Module();
        module.setName(resolved.getName());
        module.setRulesRootPath(resolved.getRulesRootPath());
        return module;
    }

    /**
     * Writes the descriptor, adding {@code rules.xml} to a project that has none.
     *
     * <p>A descriptor that comes to exactly what the file already holds is not written: the model drops a
     * block that only restates what the engine does anyway — a specification named {@code openapi.json} and
     * reconciled against, say — so asking for one of those a second time would otherwise leave the project
     * modified with nothing to show for it.
     */
    static void writeDescriptor(RulesProject project,
                                FileRoot root,
                                ProjectFilesService files,
                                ProjectDescriptor descriptor) {
        var written = descriptor.toBytes();
        if (!project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            files.createResource(root, ProjectDescriptor.FILE_NAME, new ByteArrayInputStream(written), false);
            return;
        }
        if (!Arrays.equals(written, storedDescriptor(root, files))) {
            files.updateResource(root, ProjectDescriptor.FILE_NAME, new ByteArrayInputStream(written));
        }
    }

    /** The bytes {@code rules.xml} holds now, or none where they cannot be read. */
    private static byte @Nullable [] storedDescriptor(FileRoot root, ProjectFilesService files) {
        try (var content = files.getResource(root, ProjectDescriptor.FILE_NAME, null).getContent()) {
            return content.readAllBytes();
        } catch (ProjectException | IOException | RuntimeException e) {
            return null;
        }
    }
}
