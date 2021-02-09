package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openl.rules.common.ProjectException;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.GroovyScriptFile;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.openapi.impl.OpenAPIJavaClassGenerator;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.OpenAPI;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.webstudio.service.OpenAPIHelper;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.FileNameFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project creator from OpenAPI files, generates models, spreadsheets, rules.xml, rules-deploy and compiled annotation
 * template files.
 */
public class OpenAPIProjectCreator extends AProjectCreator {
    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Logger LOGGER = LoggerFactory.getLogger(OpenAPIProjectCreator.class);

    private final ProjectFile uploadedOpenAPIFile;
    private final String comment;
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
    private final XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
    private final OpenAPIHelper openAPIHelper = new OpenAPIHelper();
    private final String repositoryId;
    private final String projectName;
    private final String modelsPath;
    private final String algorithmsPath;
    private final String modelsModuleName;
    private final String algorithmsModuleName;

    public OpenAPIProjectCreator(ProjectFile projectFile,
            String repositoryId,
            String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            String comment,
            String modelsPath,
            String algorithmsPath,
            String modelsModuleName,
            String algorithmsModuleName) throws ProjectException {
        super(projectName, projectFolder, userWorkspace);
        this.repositoryId = repositoryId;
        if (!checkFileSize(projectFile)) {
            throw new OpenAPIProjectException("Size of the file " + projectFile.getName() + " is more then 100MB.");
        }
        if (StringUtils.isBlank(modelsModuleName)) {
            throw new OpenAPIProjectException("Module name for Data Types is not provided.");
        }
        if (StringUtils.isBlank(modelsPath)) {
            throw new OpenAPIProjectException("Path for module with Data Types is not provided.");
        }
        if (StringUtils.isBlank(algorithmsModuleName)) {
            throw new OpenAPIProjectException("Module name for Rules is not provided.");
        }
        if (!NameChecker.checkName(modelsModuleName) || !NameChecker.checkName(algorithmsModuleName)) {
            throw new OpenAPIProjectException("Module " + NameChecker.BAD_NAME_MSG);
        }
        if (!NameChecker.checkName(FileUtils.getName(projectFile.getName()))) {
            throw new OpenAPIProjectException("OpenAPI File " + NameChecker.BAD_NAME_MSG);
        }

        if (modelsModuleName.equalsIgnoreCase(algorithmsModuleName)) {
            throw new OpenAPIProjectException("Module names cannot be the same.");
        }

        if (StringUtils.isBlank(algorithmsPath)) {
            throw new OpenAPIProjectException("path for module with Rules is not provided.");
        }
        if (modelsPath.startsWith("/")) {
            throw new OpenAPIProjectException("Path for Data Types cannot start with '/'");
        }
        if (algorithmsPath.startsWith("/")) {
            throw new OpenAPIProjectException("Path for Rules cannot start with '/'");
        }
        String normalizedAlgorithmsPath = FileNameFormatter.normalizePath(algorithmsPath);
        String normalizedModelsPath = FileNameFormatter.normalizePath(modelsPath);

        if (normalizedAlgorithmsPath.endsWith("/")) {
            normalizedAlgorithmsPath = normalizedAlgorithmsPath.substring(0, normalizedAlgorithmsPath.length() - 1);
        }

        if (normalizedModelsPath.endsWith("/")) {
            normalizedModelsPath = normalizedModelsPath.substring(0, normalizedModelsPath.length() - 1);
        }

        if (!FileTypeHelper.isExcelFile(normalizedAlgorithmsPath)) {
            throw new OpenAPIProjectException("Unsupported file extension for module with Rules.");
        }
        if (!FileTypeHelper.isExcelFile(normalizedModelsPath)) {
            throw new OpenAPIProjectException("Unsupported file extension for module with Data Types.");
        }

        if (normalizedAlgorithmsPath.equalsIgnoreCase(normalizedModelsPath)) {
            throw new OpenAPIProjectException("Path for Modules cannot be the same.");
        }

        this.uploadedOpenAPIFile = projectFile;
        this.comment = comment;
        this.projectName = projectName;
        this.modelsPath = normalizedModelsPath;
        this.algorithmsPath = normalizedAlgorithmsPath;
        this.modelsModuleName = modelsModuleName;
        this.algorithmsModuleName = algorithmsModuleName;
    }

    @Override
    protected RulesProjectBuilder getProjectBuilder() throws ProjectException {
        RulesProjectBuilder projectBuilder = new RulesProjectBuilder(getUserWorkspace(),
            repositoryId,
            getProjectName(),
            getProjectFolder(),
            comment);

        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        try {
            ProjectModel projectModel = getProjectModel(projectBuilder, converter);
            Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
            List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
            List<DataModel> dataModels = projectModel.getDataModels();
            EnvironmentModel environmentModel;
            boolean dataTypesArePresented = CollectionUtils.isNotEmpty(datatypeModels);
            boolean spreadsheetsArePresented = CollectionUtils.isNotEmpty(spreadsheetModels);

            if (!dataTypesArePresented && !spreadsheetsArePresented) {
                throw new OpenAPIProjectException("Uploaded file has invalid structure.");
            }

            environmentModel = new EnvironmentModel();
            environmentModel.setDependencies(Collections.singletonList(modelsModuleName));

            addFile(projectBuilder,
                openAPIHelper.generateDataTypesFile(datatypeModels),
                modelsPath,
                "Error uploading dataTypes file.");

            addFile(projectBuilder,
                openAPIHelper.generateAlgorithmsModule(spreadsheetModels, dataModels, environmentModel),
                algorithmsPath,
                "Error uploading spreadsheets file.");

            addFile(projectBuilder,
                uploadedOpenAPIFile.getInput(),
                uploadedOpenAPIFile.getName(),
                "Error uploading openAPI file.");

            OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
            boolean hasAnnotationTemplateClass = generated.hasAnnotationTemplateClass();
            if (hasAnnotationTemplateClass) {
                addGroovyScriptFile(projectBuilder, generated.getAnnotationTemplateGroovyFile());
            }
            for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
                addGroovyScriptFile(projectBuilder, groovyScriptFile);
            }

            InputStream rulesFile = generateRulesFile(hasAnnotationTemplateClass,
                projectModel.getIncludeMethodFilter());
            addFile(projectBuilder,
                rulesFile,
                ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME,
                String.format("Error uploading %s file.",
                    ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME));
            addFile(projectBuilder,
                openAPIHelper.editOrCreateRulesDeploy(serializer, projectModel, generated, null),
                RULES_DEPLOY_XML,
                "Error uploading rules-deploy.xml file.");
        } catch (Exception e) {
            projectBuilder.cancel();
            throw new ProjectException(e.getMessage(), e);
        }

        return projectBuilder;
    }

    private void addGroovyScriptFile(RulesProjectBuilder projectBuilder,
            GroovyScriptFile groovyScriptFile) throws ProjectException {
        String path = openAPIHelper.makePathToTheGeneratedFile(groovyScriptFile.getPath());
        addFile(projectBuilder,
            IOUtils.toInputStream(groovyScriptFile.getScriptText()),
            path,
            String.format("Error uploading of '%s' file.", groovyScriptFile));
    }

    private void addFile(RulesProjectBuilder projectBuilder,
            InputStream inputStream,
            String fileName,
            String errorMessage) throws ProjectException {
        try (InputStream file = inputStream) {
            projectBuilder.addFile(fileName, file);
        } catch (IOException e) {
            throw new ProjectException(errorMessage, e);
        }
    }

    private ProjectModel getProjectModel(RulesProjectBuilder projectBuilder,
            OpenAPIModelConverter converter) throws ProjectException {
        ProjectModel projectModel;
        try {
            projectModel = converter.extractProjectModel(uploadedOpenAPIFile.getTempFile().getAbsolutePath());
        } catch (IOException e) {
            projectBuilder.cancel();
            throw new ProjectException(e.getMessage(), e);
        }
        return projectModel;
    }

    private InputStream generateRulesFile(boolean genJavaClasses, Set<String> algorithmsInclude) throws IOException,
                                                                                                 ValidationException {
        ProjectDescriptor descriptor = defineDescriptor(genJavaClasses, algorithmsInclude);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            projectDescriptorManager.writeDescriptor(descriptor, baos);
            byte[] descriptorBytes = baos.toByteArray();
            return new ByteArrayInputStream(descriptorBytes);
        }
    }

    private ProjectDescriptor defineDescriptor(boolean genJavaClasses, Set<String> algorithmsInclude) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        OpenAPI openAPI = new OpenAPI();
        openAPI.setAlgorithmModuleName(algorithmsModuleName);
        openAPI.setModelModuleName(modelsModuleName);
        openAPI.setMode(OpenAPI.Mode.GENERATION);

        descriptor.setName(projectName);
        List<Module> modules = new ArrayList<>();
        Module rulesModule = new Module();
        rulesModule.setRulesRootPath(new PathEntry(algorithmsPath));
        rulesModule.setName(algorithmsModuleName);
        MethodFilter filter = new MethodFilter();
        filter.setIncludes(algorithmsInclude);
        rulesModule.setMethodFilter(filter);
        modules.add(rulesModule);

        Module modelsModule = new Module();
        modelsModule.setName(modelsModuleName);
        modelsModule.setRulesRootPath(new PathEntry(modelsPath));
        modules.add(modelsModule);

        openAPI.setPath(uploadedOpenAPIFile.getName());
        descriptor.setOpenapi(openAPI);
        descriptor.setModules(modules);

        List<PathEntry> classpath = new ArrayList<>();
        if (genJavaClasses) {
            classpath.add(new PathEntry(OpenAPIHelper.DEF_JAVA_CLASS_PATH));
        }
        descriptor.setClasspath(classpath);
        return descriptor;
    }

    @Override
    public void destroy() {
        try {
            if (!Files.deleteIfExists(uploadedOpenAPIFile.getTempFile().toPath())) {
                LOGGER.warn("Cannot delete the file {}", uploadedOpenAPIFile.getName());
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean checkFileSize(ProjectFile file) {
        return file.getSize() <= 1000 * 1024 * 1024;
    }

}
