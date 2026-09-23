package org.openl.studio.projects.service.openapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.impl.GroovyScriptFile;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.openapi.impl.OpenAPIJavaClassGenerator;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ExposedMethods;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.webstudio.service.OpenAPIHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.openapi.OpenApiGenerationPlanView;
import org.openl.studio.projects.model.openapi.OpenApiGenerationRequest;
import org.openl.studio.projects.model.openapi.OpenApiModuleView;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;

/**
 * The tables of a project, generated from an OpenAPI specification somebody wrote first.
 *
 * <p>This is the specification leading and the rules following: the operations of the specification become a
 * module of rules, its schemas become a module of data types, and what the engine needs to publish them —
 * the annotation classes, the deployment descriptor, the classpath — is written beside them.
 *
 * <p>Two modules are written, named by the caller, so that generating again writes over the same two rather
 * than leaving a second pair beside them. A module the project already reads — declared in {@code rules.xml}
 * or matched by one of its patterns — keeps its place and has its workbook replaced; one the project does not
 * read yet is added to the descriptor.
 *
 * @author Vladyslav Pikus
 */
@Service
@RequiredArgsConstructor
public class ProjectOpenApiGenerationService {

    /** Where a generated module is written when the caller says nothing, as the Editor offered it. */
    private static final String DEFAULT_FOLDER = "rules/";
    private static final String DEFAULT_EXTENSION = ".xlsx";

    private final WorkspaceProjectService projectService;
    private final ProjectFilesService filesService;
    private final ProjectFileRootFactory fileRootFactory;
    private final ProjectHistoryService history;
    private final OpenAPIHelper openApiHelper = new OpenAPIHelper();
    private final ProjectDescriptorManager descriptors = new ProjectDescriptorManager();

    /**
     * What a generation would write, so the reader can see which workbooks it replaces before it runs.
     *
     * <p>A name the project already reads a module by is answered with the workbook that module reads — that
     * is the file the generation writes over. A name it does not is answered with the workbook the generation
     * would add, named after the module.
     *
     * @param project             project the tables would be written into
     * @param algorithmModuleName what the module of rules is to be called, or blank for the default name
     * @param modelModuleName     what the module of data types is to be called, or blank for the default
     * @return the two modules as they will stand once the generation has run
     */
    public OpenApiGenerationPlanView plan(RulesProject project,
                                          @Nullable String algorithmModuleName,
                                          @Nullable String modelModuleName) {
        var resolved = projectService.getProjectDescriptor(project);
        return new OpenApiGenerationPlanView(
                planned(project, resolved, algorithmModuleName, "openapi.default.algorithm.module.name",
                        "openapi.default.algorithm.module.path"),
                planned(project, resolved, modelModuleName, "openapi.default.data.module.name",
                        "openapi.default.data.module.path"));
    }

    private static OpenApiModuleView planned(RulesProject project,
                                             ProjectDescriptor resolved,
                                             @Nullable String asked,
                                             String defaultNameKey,
                                             String defaultPathKey) {
        var name = asked == null || asked.isBlank() ? Props.text(defaultNameKey) : asked;
        var wanted = asked == null || asked.isBlank()
                ? Props.text(defaultPathKey)
                : DEFAULT_FOLDER + name + DEFAULT_EXTENSION;
        var target = targetOf(resolved, name, wanted);
        // Whether anything is lost is asked of the project, not of the descriptor: a module can be declared
        // at a workbook nobody has written yet, and generating it there takes nothing away. A file standing
        // where no module reads is not overwritten either — the generation refuses to write over what is
        // nobody's module, so saying it would be replaced would promise what cannot happen.
        return new OpenApiModuleView(name, target.path(), target.declared(),
                target.declared() && project.hasArtefact(target.path()));
    }

    /**
     * Where a module of that name reads its rules today, and whether the project already reads one.
     *
     * <p>Asked of the descriptor the engine resolved, so a module a pattern matched is the project's own as much
     * as a declared one. A name no module answers to is given the workbook asked for, and nothing is replaced.
     */
    private static Target targetOf(ProjectDescriptor resolved, String name, String wanted) {
        return modulesOf(resolved).stream()
                .filter(module -> name.equals(module.getResolvedName()))
                .map(Module::getRulesRootPath)
                .filter(Objects::nonNull)
                .findFirst()
                .map(path -> new Target(path, true))
                .orElseGet(() -> new Target(wanted, false));
    }

    /** The workbook a generated module is written to, and whether the project already reads that module. */
    private record Target(String path, boolean declared) {
    }

    /**
     * Writes the project's tables from the specification it names.
     *
     * <p>The compilation is held for the whole write: the workbooks the modules read are replaced under it,
     * and a compilation reading them half-written answers for neither the old tables nor the new.
     *
     * @param project project to write the tables into
     * @param request the specification to read, and the two modules to write
     */
    public void generateTables(RulesProject project, OpenApiGenerationRequest request) {
        requireWritableNames(request);
        var root = fileRootFactory.of(project);
        var resolved = projectService.getProjectDescriptor(project);
        var algorithm = targetOf(resolved, request.algorithmModuleName(), request.algorithmModulePath());
        var model = targetOf(resolved, request.modelModuleName(), request.modelModulePath());
        // Asked before anything is written: a workbook standing where a new module would go is the author's,
        // and a refusal halfway through would leave one module generated and the other not.
        requireWorkbook(algorithm);
        requireWorkbook(model);
        requireTwoWorkbooks(algorithm, model);
        requireFree(project, algorithm);
        requireFree(project, model);
        var descriptor = ProjectOpenApiService.descriptorToWrite(project, root, filesService, resolved);
        var specification = readSpecification(project, request.path());

        var studio = projectService.getWebStudio();
        // What the two modules read before the write is kept, so a generation over an existing module can be
        // taken back from the project's own history. Kept for these two workbooks rather than through the
        // session's own history calls: those work on the project the session has open, which a request about
        // another project would have snapshotted instead.
        history.keepBeforeWrite(project, algorithm.path());
        history.keepBeforeWrite(project, model.path());
        studio.freezeProject(project.getName());
        try {
            var generated = new OpenAPIJavaClassGenerator(specification).generate();
            write(project, root, model, () -> openApiHelper.generateDataTypesFile(specification));
            write(project, root, algorithm, () -> openApiHelper.generateAlgorithmsModule(
                    specification.getSpreadsheetResultModels(), specification.getDataModels(),
                    dependingOn(request.modelModuleName())));
            writeGeneratedClasses(root, generated);
            writeRulesDeploy(project, root, specification, generated);
            declareGeneration(project, root, descriptor, request, algorithm, model, specification,
                    generated.hasAnnotationTemplateClass());
            history.recordWritten(project, algorithm.path());
            history.recordWritten(project, model.path());
        } finally {
            studio.releaseProject(project.getName());
        }
        // The session resolved and compiled the project as it stood before the generation: its module list,
        // its descriptor and its compiled tables all answer for workbooks that are no longer there.
        studio.reset();
    }

    /**
     * A module is read from an Excel workbook, so that is what a generated one is written to.
     *
     * <p>Asked of the workbook the generation will write, as the neighbouring flow asks it of the paths a
     * project is created from: a file named anything else is served as that kind of file and read as no
     * module at all, while {@code rules.xml} names it as one.
     */
    private static void requireWorkbook(Target target) {
        if (!FileTypeHelper.isExcelFile(target.path())) {
            throw new ConflictException("projects.openapi.module-path.not-a-workbook.message", target.path());
        }
    }

    /** A module the project does not read yet cannot be written where a file already stands. */
    private static void requireFree(RulesProject project, Target target) {
        if (!target.declared() && project.hasArtefact(target.path())) {
            throw new ConflictException("projects.openapi.path-taken.message", target.path());
        }
    }

    /** The names have to be ones a repository can hold, and the two modules have to be two. */
    private static void requireWritableNames(OpenApiGenerationRequest request) {
        if (!NameChecker.checkName(request.algorithmModuleName()) || !NameChecker.checkName(request.modelModuleName())) {
            throw new ConflictException("projects.openapi.module-name.invalid.message",
                    NameChecker.getForbiddenCharacters());
        }
        if (request.algorithmModuleName().equals(request.modelModuleName())) {
            throw new ConflictException("projects.openapi.module-name.same.message");
        }
    }

    /**
     * The two workbooks have to be two.
     *
     * <p>Asked of the workbooks the generation will write rather than of the ones the request names: a module
     * the project already reads is written where it reads, whatever workbook was asked for it.
     */
    private static void requireTwoWorkbooks(Target algorithm, Target model) {
        if (algorithm.path().equalsIgnoreCase(model.path())) {
            throw new ConflictException("projects.openapi.module-path.same.message");
        }
    }

    /**
     * The specification as the scaffolding reads it.
     *
     * <p>Read from the working copy on disk: the converter is the engine's, and it opens the file by path.
     * A project nobody has checked out has no copy to read, and is refused rather than read from the design
     * repository behind the reader's back.
     */
    private ProjectModel readSpecification(RulesProject project, String path) {
        var folder = localFolder(project);
        if (folder == null) {
            throw new ConflictException("projects.openapi.not-checked-out.message");
        }
        var file = folder.resolve(path.replace('\\', '/')).normalize();
        if (!file.startsWith(folder) || !Files.exists(file)) {
            throw new ConflictException("projects.openapi.specification-missing.message", path);
        }
        try {
            return new OpenAPIScaffoldingConverter().extractProjectModel(file.toString());
        } catch (Exception e) {
            throw new ConflictException("projects.openapi.specification-unreadable.message", path);
        }
    }

    private static @Nullable Path localFolder(RulesProject project) {
        var name = project.getLocalFolderName();
        if (name == null) {
            return null;
        }
        var folder = project.getLocalRepository().getRoot().resolve(name).normalize();
        return Files.isDirectory(folder) ? folder : null;
    }

    /**
     * Writes one generated workbook, over the one standing there or beside nothing at all.
     *
     * <p>A module the project already reads has its workbook replaced where it reads it, so a generation run
     * again leaves one workbook rather than two.
     */
    private void write(RulesProject project, FileRoot root, Target target, Generator content) {
        try (var workbook = content.open()) {
            if (project.hasArtefact(target.path())) {
                filesService.updateResource(root, target.path(), workbook);
            } else {
                filesService.createResource(root, target.path(), workbook, true);
            }
        } catch (IOException e) {
            throw new ConflictException("projects.openapi.write-failed.message", target.path());
        }
    }

    /** The module of rules reads the data types the other module holds. */
    private static EnvironmentModel dependingOn(String modelModuleName) {
        var environment = new EnvironmentModel();
        environment.setDependencies(Collections.singletonList(modelModuleName));
        return environment;
    }

    /**
     * The classes the engine needs to publish the generated rules under the names the specification uses.
     *
     * <p>What a previous generation left is dropped first: a class nobody generates any more would otherwise
     * stay on the classpath, naming operations the specification no longer has.
     */
    private void writeGeneratedClasses(FileRoot root, OpenAPIGeneratedClasses generated) {
        var folder = OpenAPIHelper.DEF_JAVA_CLASS_PATH + "/"
                + OpenAPIJavaClassGenerator.DEFAULT_OPEN_API_PATH.replace(".", "/");
        try {
            filesService.deleteResource(root, folder);
        } catch (NotFoundException gone) {
            // Nothing was generated before, so there is nothing to drop. Anything else — a folder that
            // cannot be written to — is the caller's to hear about: a class left there names operations the
            // specification no longer has.
        }
        if (generated.hasAnnotationTemplateClass()) {
            writeScript(root, generated.getAnnotationTemplateGroovyFile());
        }
        generated.getGroovyCommonClasses().forEach(script -> writeScript(root, script));
    }

    private void writeScript(FileRoot root, GroovyScriptFile script) {
        var path = openApiHelper.makePathToTheGeneratedFile(script.getPath());
        filesService.createResource(root, path,
                new ByteArrayInputStream(script.getScriptText().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                true);
    }

    /** The deployment descriptor, which names the interface the generated rules are published under. */
    private void writeRulesDeploy(RulesProject project,
                                  FileRoot root,
                                  ProjectModel specification,
                                  OpenAPIGeneratedClasses generated) {
        var kept = project.hasArtefact(RulesDeploy.FILE_NAME) ? readRulesDeploy(root) : null;
        try (var written = openApiHelper.editOrCreateRulesDeploy(specification, generated, kept)) {
            if (kept == null) {
                filesService.createResource(root, RulesDeploy.FILE_NAME, written, false);
            } else {
                filesService.updateResource(root, RulesDeploy.FILE_NAME, written);
            }
        } catch (IOException e) {
            throw new ConflictException("projects.openapi.write-failed.message", RulesDeploy.FILE_NAME);
        }
    }

    private @Nullable RulesDeploy readRulesDeploy(FileRoot root) {
        try (var content = filesService.getResource(root, RulesDeploy.FILE_NAME, null).getContent()) {
            return RulesDeploy.read(content);
        } catch (Exception e) {
            throw new ConflictException("projects.openapi.rules-deploy-unreadable.message");
        }
    }

    /**
     * Leaves {@code rules.xml} declaring what the generation wrote: the specification it is generated from,
     * the two modules, the methods the specification exposes, and the classpath the generated classes need.
     */
    private void declareGeneration(RulesProject project,
                                   FileRoot root,
                                   ProjectDescriptor descriptor,
                                   OpenApiGenerationRequest request,
                                   Target algorithm,
                                   Target model,
                                   ProjectModel specification,
                                   boolean hasGeneratedClasses) {
        var openapi = new OpenAPI();
        openapi.setPath(request.path());
        openapi.setMode(OpenAPI.Mode.GENERATION);
        openapi.setAlgorithmModuleName(request.algorithmModuleName());
        openapi.setModelModuleName(request.modelModuleName());
        descriptor.setOpenapi(openapi);

        declare(descriptor, request.algorithmModuleName(), algorithm.path());
        declare(descriptor, request.modelModuleName(), model.path());

        if (!specification.getIncludeMethodFilter().isEmpty()) {
            var exposed = new ExposedMethods();
            exposed.setIncludes(new LinkedHashSet<>(specification.getIncludeMethodFilter()));
            descriptor.setExposedMethods(exposed);
        }
        declareClassPath(descriptor, hasGeneratedClasses);
        ProjectOpenApiService.writeDescriptor(project, root, filesService, descriptor);
    }

    /**
     * Declares a generated module, unless the descriptor already leads to it — by the rule the rest of Studio
     * registers a module with, which leaves a pattern-matched one auto-discovered.
     */
    private void declare(ProjectDescriptor descriptor, String name, String path) {
        var module = new Module();
        module.setName(name);
        module.setRulesRootPath(path);
        descriptors.registerModule(descriptor, module);
    }

    /** The generated classes are read from the classpath, and are on it exactly while there are some. */
    private static void declareClassPath(ProjectDescriptor descriptor, boolean hasGeneratedClasses) {
        var classpath = descriptor.getClasspath() == null ? new ArrayList<String>()
                : new ArrayList<>(descriptor.getClasspath());
        var declared = classpath.contains(OpenAPIHelper.DEF_JAVA_CLASS_PATH);
        if (hasGeneratedClasses && !declared) {
            classpath.add(OpenAPIHelper.DEF_JAVA_CLASS_PATH);
        } else if (!hasGeneratedClasses && declared) {
            classpath.removeIf(OpenAPIHelper.DEF_JAVA_CLASS_PATH::equals);
        } else {
            return;
        }
        descriptor.setClasspath(classpath);
    }

    private static List<Module> modulesOf(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        return CollectionUtils.isEmpty(modules) ? List.of() : modules;
    }

    /** Opens a generated workbook, which is written as it is read. */
    @FunctionalInterface
    private interface Generator {
        InputStream open() throws IOException;
    }
}
