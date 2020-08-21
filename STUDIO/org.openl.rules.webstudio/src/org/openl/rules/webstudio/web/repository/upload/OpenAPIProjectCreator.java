package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ValidationException;
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
    public static final String MODELS_FILE_NAME = "Model.xlsx";
    public static final String SPR_FILE_NAME = "Algorithm.xlsx";
    public static final String RULES_FILE_NAME = "rules.xml";
    public static final String MODULE_NAME = "Main";
    public static final String OPENAPI_FILE_DEFAULT_NAME = "openapi";

    private final Logger log = LoggerFactory.getLogger(OpenAPIProjectCreator.class);

    private final File uploadedOpenAPIFile;
    private final String comment;
    private final String fileName;
    private final ProjectDescriptorManager projectDescriptorManager = new ProjectDescriptorManager();
    private final String projectName;

    public OpenAPIProjectCreator(String openAPIFileName,
            InputStream uploadedFile,
            long fileSize,
            String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            String comment) throws ProjectException {
        super(projectName, projectFolder, userWorkspace);
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
            getProjectName(),
            getProjectFolder(),
            comment);

        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        try {
            ProjectModel projectModel = converter.extractProjectModel(uploadedOpenAPIFile.getAbsolutePath());
            List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
            List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();

            boolean dataTypesAreEmpty = CollectionUtils.isEmpty(datatypeModels);
            boolean sprsAreEmpty = CollectionUtils.isEmpty(spreadsheetResultModels);

            if (dataTypesAreEmpty && sprsAreEmpty) {
                throw new ProjectException("There are no data types and spreadsheets are found.");
            }
            if (!dataTypesAreEmpty) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ExcelFileBuilder.generateDataTypes(datatypeModels, bos);
                byte[] dtBytes = bos.toByteArray();
                InputStream dtis = new ByteArrayInputStream(dtBytes);

                projectBuilder.addFile(PATH + MODELS_FILE_NAME, dtis);
            }
            if (!sprsAreEmpty) {
                ByteArrayOutputStream sos = new ByteArrayOutputStream();
                ExcelFileBuilder.generateSpreadsheets(spreadsheetResultModels, sos);
                byte[] sprBytes = sos.toByteArray();
                InputStream spris = new ByteArrayInputStream(sprBytes);
                projectBuilder.addFile(PATH + SPR_FILE_NAME, spris);
            }

            projectBuilder.addFile(fileName, new FileInputStream(uploadedOpenAPIFile));

            ProjectDescriptor descriptor = defineDescriptor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            projectDescriptorManager.writeDescriptor(descriptor, baos);
            byte[] descriptorBytes = baos.toByteArray();
            InputStream ds = new ByteArrayInputStream(descriptorBytes);
            projectBuilder.addFile(RULES_FILE_NAME, ds);

        } catch (IOException | ValidationException e) {
            projectBuilder.cancel();
            throw new ProjectException(e.getMessage(), e);
        }
        return projectBuilder;
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
                log.warn("Cannot delete the file {}", uploadedOpenAPIFile.getName());
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean checkFileSize(long size) {
        return size <= 1000 * 1024 * 1024;
    }
}
