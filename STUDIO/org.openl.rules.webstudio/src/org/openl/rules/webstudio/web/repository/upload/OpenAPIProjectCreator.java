package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.CollectionUtils;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project creator from OpenAPI files, generates models, spreadsheets and rules.xml files.
 */
public class OpenAPIProjectCreator extends AProjectCreator {
    public static final String PATH = "rules/";
    public static final String MODELS_NAME = "Model";
    public static final String MODELS_FILE_NAME = MODELS_NAME + ".xlsx";
    public static final String SPR_FILE_NAME = "Algorithm.xlsx";
    public static final String RULES_FILE_NAME = "rules.xml";
    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";
    public static final String MODULE_NAME = "Main";
    public static final String OPENAPI_FILE_DEFAULT_NAME = "openapi";

    private final Logger LOGGER = LoggerFactory.getLogger(OpenAPIProjectCreator.class);

    private final File uploadedOpenAPIFile;
    private final String comment;
    private final String fileName;
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
    private final XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
    private final String repositoryId;
    private final String projectName;

    public OpenAPIProjectCreator(String openAPIFileName,
            InputStream uploadedFile,
            long fileSize,
            String repositoryId,
            String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            String comment) throws ProjectException {
        super(projectName, projectFolder, userWorkspace);
        this.repositoryId = repositoryId;
        String filteredName = FileUtils.removeExtension(openAPIFileName);
        if (!checkFileSize(fileSize)) {
            throw new ProjectException("Size of the file " + uploadedFile + " is more then 100MB.");
        }
        if (!filteredName.equalsIgnoreCase(OPENAPI_FILE_DEFAULT_NAME)) {
            throw new ProjectException("Only files with name 'openapi' and formats JSON, YML/YAML are accepted.");
        }
        this.uploadedOpenAPIFile = FileTool.toTempFile(uploadedFile, openAPIFileName);
        this.fileName = openAPIFileName;
        this.comment = comment;
        this.projectName = projectName;
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
            List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
            List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
            EnvironmentModel environmentModel = null;
            boolean dataTypesAreEmpty = CollectionUtils.isEmpty(datatypeModels);
            boolean sprsAreEmpty = CollectionUtils.isEmpty(spreadsheetModels);

            if (!dataTypesAreEmpty) {
                environmentModel = new EnvironmentModel();
                environmentModel.setDependencies(Collections.singletonList(MODELS_NAME));
            }

            if (dataTypesAreEmpty && sprsAreEmpty) {
                throw new ProjectException("Error creating the project, uploaded file has invalid structure.");
            }
            if (!dataTypesAreEmpty) {
                addFile(projectBuilder,
                    generateDataTypesFile(datatypeModels),
                    PATH + MODELS_FILE_NAME,
                    "Error uploading dataTypes file.");
            }
            if (!sprsAreEmpty) {
                addFile(projectBuilder,
                    generateSpreadsheetsFile(spreadsheetModels, environmentModel),
                    PATH + SPR_FILE_NAME,
                    "Error uploading spreadsheets file.");
            }
            InputStream uploadedFile = new FileInputStream(uploadedOpenAPIFile);
            addFile(projectBuilder, uploadedFile, fileName, "Error uploading openAPI file.");
            InputStream rulesFile = generateRulesFile();
            addFile(projectBuilder, rulesFile, RULES_FILE_NAME, "Error uploading rules.xml file.");
            addFile(projectBuilder,
                generateRulesDeployFile(projectModel),
                RULES_DEPLOY_XML,
                "Error uploading rules-deploy.xml file.");
        } catch (Exception e) {
            projectBuilder.cancel();
            throw new ProjectException(e.getMessage(), e);
        }
        return projectBuilder;
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
            projectModel = converter.extractProjectModel(uploadedOpenAPIFile.getAbsolutePath());
        } catch (IOException e) {
            projectBuilder.cancel();
            throw new ProjectException(e.getMessage(), e);
        }
        return projectModel;
    }

    private InputStream generateDataTypesFile(List<DatatypeModel> datatypeModels) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelFileBuilder.generateDataTypes(datatypeModels, bos);
        byte[] dtBytes = bos.toByteArray();
        return new ByteArrayInputStream(dtBytes);
    }

    private InputStream generateSpreadsheetsFile(List<SpreadsheetModel> spreadsheetModels,
            EnvironmentModel environmentModel) {
        ByteArrayOutputStream sos = new ByteArrayOutputStream();
        ExcelFileBuilder.generateSpreadsheetsWithEnvironment(spreadsheetModels, sos, environmentModel);
        byte[] sprBytes = sos.toByteArray();
        return new ByteArrayInputStream(sprBytes);
    }

    private ByteArrayInputStream generateRulesDeployFile(ProjectModel projectModel) {
        RulesDeploy rd = new RulesDeploy();
        rd.setProvideRuntimeContext(projectModel.isRuntimeContextProvided());
        rd.setPublishers(new RulesDeploy.PublisherType[] { RulesDeploy.PublisherType.RESTFUL });
        return new ByteArrayInputStream(serializer.serialize(rd).getBytes(StandardCharsets.UTF_8));
    }

    private InputStream generateRulesFile() throws IOException, ValidationException {
        ProjectDescriptor descriptor = defineDescriptor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        projectDescriptorManager.writeDescriptor(descriptor, baos);
        byte[] descriptorBytes = baos.toByteArray();
        return new ByteArrayInputStream(descriptorBytes);
    }

    private ProjectDescriptor defineDescriptor() {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(projectName);
        List<Module> modules = new ArrayList<>();
        Module rulesModule = new Module();
        rulesModule.setRulesRootPath(new PathEntry(PATH + "*.xlsx"));
        rulesModule.setName(MODULE_NAME);
        modules.add(rulesModule);
        descriptor.setModules(modules);
        return descriptor;
    }

    @Override
    public void destroy() {
        try {
            if (!Files.deleteIfExists(uploadedOpenAPIFile.toPath())) {
                LOGGER.warn("Cannot delete the file {}", uploadedOpenAPIFile.getName());
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean checkFileSize(long size) {
        return size <= 1000 * 1024 * 1024;
    }
}
