package org.openl.studio.projects.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import org.openl.CompiledOpenClass;
import org.openl.rules.common.ProjectException;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.impl.GroovyScriptFile;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.openapi.impl.OpenAPIJavaClassGenerator;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.openapi.OpenApiGenerationException;
import org.openl.rules.project.openapi.OpenApiGenerator;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.service.OpenAPIHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.OpenApiTablesRequest;
import org.openl.studio.projects.model.ProjectDescriptorView;
import org.openl.studio.projects.model.ProjectDescriptorView.ExposedMethodView;
import org.openl.studio.projects.model.ProjectDescriptorView.ModuleView;
import org.openl.studio.projects.model.ProjectDescriptorView.OpenApiView;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.FileNameFormatter;
import org.openl.validation.ValidatedCompiledOpenClass;

/**
 * Generates an OpenAPI schema from a project's compiled rules and datatypes, writes it as a project
 * file, and points the descriptor's {@code <openapi>} at it. This is the server side of the Project
 * page's "Generate OpenAPI schema" action.
 *
 * @author Yury Molchan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectOpenApiService {

    @Qualifier("designRepositoryAclService")
    private final RepositoryAclService designRepositoryAclService;
    private final WorkspaceProjectService workspaceProjectService;
    private final ProjectDescriptorService projectDescriptorService;

    /**
     * Generates the OpenAPI schema from the compiled project, writes it as a project file, and points
     * the descriptor's {@code <openapi>} at it in reconciliation mode.
     *
     * <p>Requires design-repository WRITE permission. The project must have at least one module and
     * must compile without errors. When the project already has an OpenAPI file, it is overwritten
     * and its format is kept; otherwise {@code openapi.json} is created.
     *
     * @param project the project to generate the schema for; WRITE access is enforced here
     * @return the descriptor view read back after the schema and {@code <openapi>} are written
     */
    public ProjectDescriptorView generateSchema(RulesProject project) {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("project.descriptor.write.forbidden.message");
        }
        ProjectDescriptorView current = projectDescriptorService.getDescriptor(project);
        // The project's modules may be declared in rules.xml or auto-discovered from its files, so the
        // authoritative "has modules" check is the compiled model, not the raw descriptor.
        ProjectModel model = workspaceProjectService.openProject(project).awaitCompiled();
        if (model.getModuleInfo() == null) {
            throw new ConflictException("project.openapi.generate.no.modules.message");
        }
        if (hasCompilationErrors(model)) {
            throw new ConflictException("project.openapi.generate.compilation.error.message");
        }
        String existingPath = existingSchemaPath(project, current.openapi());
        OpenAPI.Type type = schemaType(existingPath);
        String path = existingPath != null ? existingPath : type.getDefaultFileName();
        writeSchema(project, path, generate(model, type), existingPath != null);
        writeReconciliationConfig(project, current, path);
        return projectDescriptorService.getDescriptor(project);
    }

    private byte[] generate(ProjectModel model, OpenAPI.Type type) {
        var descriptor = model.getModuleInfo().getProject();
        try {
            OpenApiGenerator generator = OpenApiGenerator
                    .builder(descriptor, model.getRulesInstantiationStrategy(descriptor))
                    .generator();
            io.swagger.v3.oas.models.OpenAPI schema = generator.generate();
            String text = switch (type) {
                case JSON -> Json.pretty().writeValueAsString(schema);
                case YAML, YML -> Yaml.pretty().writeValueAsString(schema);
            };
            return text.getBytes(StandardCharsets.UTF_8);
        } catch (OpenApiGenerationException | RulesInstantiationException | JsonProcessingException e) {
            log.debug("OpenAPI schema generation failed", e);
            throw new ConflictException("project.openapi.generate.failed.message");
        }
    }

    private void writeSchema(RulesProject project, String path, byte[] content, boolean update) {
        try {
            if (update) {
                var resource = (AProjectResource) project.getArtefact(path);
                resource.setContent(new ByteArrayInputStream(content));
            } else {
                project.addResource(path, new ByteArrayInputStream(content));
                var artefact = project.getArtefact(path);
                if (!designRepositoryAclService.hasAcl(artefact)) {
                    designRepositoryAclService.createAcl(artefact,
                            List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
                }
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.openapi.generate.write.failed.message");
        }
    }

    private void writeReconciliationConfig(RulesProject project, ProjectDescriptorView current, String path) {
        OpenApiView previous = current.openapi();
        boolean noModuleOverrides = previous == null
                || (StringUtils.isBlank(previous.modelModuleName()) && StringUtils.isBlank(previous.algorithmModuleName()));
        // The default openapi file (openapi.json/yaml/yml) is reconciled automatically, so no <openapi>
        // element is needed. Skipping the write also avoids materializing a rules.xml for a project whose
        // modules are auto-discovered — writing one would drop those modules. A non-default config only
        // exists when rules.xml already does, so the round-trip below preserves its modules.
        if (isDefaultSchemaPath(path) && noModuleOverrides) {
            return;
        }
        var openapi = new OpenApiView(
                path,
                "RECONCILIATION",
                previous == null ? null : previous.modelModuleName(),
                previous == null ? null : previous.algorithmModuleName());
        var updated = new ProjectDescriptorView(
                current.name(), current.comment(), current.modules(), current.dependencies(),
                current.classpath(), openapi, current.exposedMethods(),
                current.propertiesFileNameProcessor(), current.propertiesFileNamePatterns(),
                current.editable(), current.contentHash());
        projectDescriptorService.updateDescriptor(project, updated, true);
    }

    private static boolean isDefaultSchemaPath(String path) {
        return Stream.of(OpenAPI.Type.values()).anyMatch(type -> type.getDefaultFileName().equals(path));
    }

    private static boolean hasCompilationErrors(ProjectModel model) {
        CompiledOpenClass compiled = model.getCompiledOpenClass();
        if (compiled instanceof ValidatedCompiledOpenClass validated) {
            return validated.hasErrors() && !validated.hasOnlyValidationErrors();
        }
        return compiled.hasErrors();
    }

    private static @Nullable String existingSchemaPath(RulesProject project, @Nullable OpenApiView openapi) {
        if (openapi != null && StringUtils.isNotBlank(openapi.path()) && project.hasArtefact(openapi.path())) {
            return openapi.path();
        }
        return Stream.of(OpenAPI.Type.values())
                .map(OpenAPI.Type::getDefaultFileName)
                .filter(project::hasArtefact)
                .findFirst()
                .orElse(null);
    }

    private static OpenAPI.Type schemaType(@Nullable String existingPath) {
        if (existingPath == null) {
            return OpenAPI.Type.JSON;
        }
        OpenAPI.Type type = OpenAPI.Type.chooseType(FileUtils.getExtension(existingPath));
        return type == null ? OpenAPI.Type.JSON : type;
    }

    /**
     * Generates OpenL rules and datatype tables from an OpenAPI spec already committed to the project,
     * and points the descriptor's {@code <openapi>} at the spec in generation mode.
     *
     * <p>Requires design-repository WRITE permission. The spec at {@code specPath} is converted into a
     * rules module and a datatype module (written to the requested paths); the generated Groovy
     * classes and {@code rules-deploy.xml} are written; and the descriptor is updated with the two
     * modules, the {@code <openapi>} generation config, the classpath and the exposed methods.
     *
     * @param project the project to generate into; WRITE access is enforced here
     * @param request the spec path plus the target module names and paths
     * @return the descriptor view read back after generation
     */
    public ProjectDescriptorView generateTables(RulesProject project, OpenApiTablesRequest request) {
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.WRITE))) {
            throw new ForbiddenException("project.descriptor.write.forbidden.message");
        }
        if (!NameChecker.checkName(request.rulesModuleName()) || !NameChecker.checkName(request.dataModuleName())) {
            throw new ConflictException("project.openapi.tables.module.name.invalid.message");
        }
        String rulesPath = FileNameFormatter.normalizePath(request.rulesModulePath());
        String dataPath = FileNameFormatter.normalizePath(request.dataModulePath());
        if (rulesPath.equalsIgnoreCase(dataPath)) {
            throw new ConflictException("project.openapi.tables.module.path.conflict.message");
        }
        String specPath = FileNameFormatter.normalizePath(request.specPath());
        if (!project.hasArtefact(specPath)) {
            throw new ConflictException("project.openapi.tables.spec.not.found.message");
        }

        ProjectDescriptorView current = projectDescriptorService.getDescriptor(project);
        ModuleView existingRules = findModule(current, request.rulesModuleName());
        ModuleView existingData = findModule(current, request.dataModuleName());
        checkTargetPath(project, rulesPath, existingRules == null);
        checkTargetPath(project, dataPath, existingData == null);

        var model = extractSpecModel(project, specPath);

        deleteExistingModuleFile(project, existingRules);
        deleteExistingModuleFile(project, existingData);
        writeDataTypesModule(project, dataPath, model);
        writeRulesModule(project, request.dataModuleName(), rulesPath, model);

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(model).generate();
        boolean annotationTemplate = generated.hasAnnotationTemplateClass();
        deletePreviouslyGeneratedClasses(project);
        writeGeneratedClasses(project, generated, annotationTemplate);
        writeRulesDeploy(project, model, generated);

        writeGenerationDescriptor(project, current, request, specPath, rulesPath, dataPath,
                existingRules == null, existingData == null, model, annotationTemplate);
        return projectDescriptorService.getDescriptor(project);
    }

    private static @Nullable ModuleView findModule(ProjectDescriptorView view, String name) {
        return view.modules().stream().filter(module -> name.equals(module.name())).findFirst().orElse(null);
    }

    private static void checkTargetPath(RulesProject project, String path, boolean isNew) {
        if (isNew && project.hasArtefact(path)) {
            throw new ConflictException("project.openapi.tables.file.exists.message");
        }
    }

    private org.openl.rules.model.scaffolding.ProjectModel extractSpecModel(RulesProject project, String specPath) {
        Path temp = null;
        try {
            var spec = (AProjectResource) project.getArtefact(specPath);
            temp = Files.createTempFile("openl-openapi", "." + FileUtils.getExtension(specPath));
            try (InputStream in = spec.getContent()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            return new OpenAPIScaffoldingConverter().extractProjectModel(temp.toString());
        } catch (Exception e) {
            log.debug("Failed to read the OpenAPI spec {}", specPath, e);
            throw new ConflictException("project.openapi.tables.spec.invalid.message");
        } finally {
            deleteQuietly(temp);
        }
    }

    private static void deleteQuietly(@Nullable Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.debug("Failed to delete the temporary OpenAPI spec {}", path, e);
            }
        }
    }

    private void writeDataTypesModule(RulesProject project, String path, org.openl.rules.model.scaffolding.ProjectModel model) {
        try (InputStream content = new OpenAPIHelper().generateDataTypesFile(model.getDatatypeModels())) {
            project.addResource(path, content);
        } catch (IOException | ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
        grantContributorAcl(project, path);
    }

    private void writeRulesModule(RulesProject project, String dataModuleName, String path,
                                  org.openl.rules.model.scaffolding.ProjectModel model) {
        var environment = new EnvironmentModel();
        environment.setDependencies(Collections.singletonList(dataModuleName));
        try (InputStream content = new OpenAPIHelper()
                .generateAlgorithmsModule(model.getSpreadsheetResultModels(), model.getDataModels(), environment)) {
            project.addResource(path, content);
        } catch (IOException | ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
        grantContributorAcl(project, path);
    }

    private void grantContributorAcl(RulesProject project, String path) {
        try {
            var artefact = project.getArtefact(path);
            if (!designRepositoryAclService.hasAcl(artefact)) {
                designRepositoryAclService.createAcl(artefact,
                        List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
    }

    private void deletePreviouslyGeneratedClasses(RulesProject project) {
        try {
            project.deleteArtefactsInFolder(OpenAPIHelper.DEF_JAVA_CLASS_PATH + "/"
                    + OpenAPIJavaClassGenerator.DEFAULT_OPEN_API_PATH.replace(".", "/"));
        } catch (ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
    }

    private void writeGeneratedClasses(RulesProject project, OpenAPIGeneratedClasses generated, boolean annotationTemplate) {
        var helper = new OpenAPIHelper();
        try {
            if (annotationTemplate) {
                addGroovy(project, helper, generated.getAnnotationTemplateGroovyFile());
            }
            for (GroovyScriptFile groovy : generated.getGroovyCommonClasses()) {
                addGroovy(project, helper, groovy);
            }
        } catch (ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
    }

    private static void addGroovy(RulesProject project, OpenAPIHelper helper, GroovyScriptFile groovy) throws ProjectException {
        byte[] content = groovy.getScriptText().getBytes(StandardCharsets.UTF_8);
        project.addResource(helper.makePathToTheGeneratedFile(groovy.getPath()), new ByteArrayInputStream(content));
    }

    private void writeRulesDeploy(RulesProject project, org.openl.rules.model.scaffolding.ProjectModel model,
                                  OpenAPIGeneratedClasses generated) {
        var helper = new OpenAPIHelper();
        try {
            if (project.hasArtefact(RulesDeploy.FILE_NAME)) {
                var artefact = (AProjectResource) project.getArtefact(RulesDeploy.FILE_NAME);
                try (InputStream content = artefact.getContent()) {
                    RulesDeploy existing = RulesDeploy.read(content);
                    artefact.setContent(helper.editOrCreateRulesDeploy(model, generated, existing));
                }
            } else {
                try (InputStream content = helper.editOrCreateRulesDeploy(model, generated, null)) {
                    project.addResource(RulesDeploy.FILE_NAME, content);
                }
            }
        } catch (ProjectException | IOException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
    }

    private void deleteExistingModuleFile(RulesProject project, @Nullable ModuleView module) {
        if (module == null || module.rulesRootPath() == null || !project.hasArtefact(module.rulesRootPath())) {
            return;
        }
        try {
            AProjectArtefact artefact = project.getArtefact(module.rulesRootPath());
            if (!designRepositoryAclService.isGranted(artefact, true, BasePermission.DELETE)) {
                throw new ForbiddenException("project.openapi.tables.delete.forbidden.message");
            }
            project.deleteArtefact(artefact.getInternalPath());
        } catch (ProjectException e) {
            throw new ConflictException("project.openapi.tables.write.failed.message");
        }
    }

    private void writeGenerationDescriptor(RulesProject project, ProjectDescriptorView current, OpenApiTablesRequest request,
                                           String specPath, String rulesPath, String dataPath,
                                           boolean newRules, boolean newData,
                                           org.openl.rules.model.scaffolding.ProjectModel model, boolean annotationTemplate) {
        var modules = new ArrayList<>(current.modules());
        if (newRules) {
            modules.add(new ModuleView(request.rulesModuleName(), rulesPath, null, false, false));
        }
        if (newData) {
            modules.add(new ModuleView(request.dataModuleName(), dataPath, null, false, false));
        }
        var openapi = new OpenApiView(specPath, "GENERATION", request.dataModuleName(), request.rulesModuleName());
        var classpath = new ArrayList<>(current.classpath());
        classpath.remove(OpenAPIHelper.DEF_JAVA_CLASS_PATH);
        if (annotationTemplate) {
            classpath.add(OpenAPIHelper.DEF_JAVA_CLASS_PATH);
        }
        List<ExposedMethodView> exposed = current.exposedMethods();
        if (!model.getIncludeMethodFilter().isEmpty()) {
            exposed = model.getIncludeMethodFilter().stream()
                    .map(pattern -> new ExposedMethodView(pattern, "include"))
                    .toList();
        }
        var updated = new ProjectDescriptorView(current.name(), current.comment(), modules, current.dependencies(),
                classpath, openapi, exposed, current.propertiesFileNameProcessor(), current.propertiesFileNamePatterns(),
                current.editable(), current.contentHash());
        projectDescriptorService.updateDescriptor(project, updated, true);
    }
}
