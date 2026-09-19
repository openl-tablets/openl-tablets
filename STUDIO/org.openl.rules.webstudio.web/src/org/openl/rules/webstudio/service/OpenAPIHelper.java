package org.openl.rules.webstudio.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class OpenAPIHelper {

    public static final String DEF_JAVA_CLASS_PATH = "classes";

    public InputStream generateAlgorithmsModule(final List<SpreadsheetModel> spreadsheetModels,
                                                final List<DataModel> dataModels,
                                                final EnvironmentModel environmentModel) throws IOException {
        try (var sos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(spreadsheetModels, dataModels, sos, environmentModel);
            var sprBytes = sos.toByteArray();
            return new ByteArrayInputStream(sprBytes);
        }
    }

    /** The model module: the vocabularies and the data types of the project. */
    public InputStream generateDataTypesFile(final ProjectModel projectModel) throws IOException {
        try (var bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(projectModel, bos);
            var dtBytes = bos.toByteArray();
            return new ByteArrayInputStream(dtBytes);
        }
    }

    public InputStream editOrCreateRulesDeploy(final ProjectModel projectModel,
                                               final OpenAPIGeneratedClasses generated,
                                               RulesDeploy exitingRulesDeploy) throws IOException {
        var fileExists = exitingRulesDeploy != null;
        RulesDeploy rd = fileExists ? exitingRulesDeploy : new RulesDeploy();
        if (generated.hasAnnotationTemplateClass()) {
            rd.setAnnotationTemplateClassName(generated.getAnnotationTemplateGroovyFile().getNameWithPackage());
        } else {
            if (StringUtils.isNotBlank(rd.getAnnotationTemplateClassName())) {
                rd.setAnnotationTemplateClassName(null);
            }
        }
        rd.setProvideRuntimeContext(projectModel.isRuntimeContextProvided());
        if (CollectionUtils.isEmpty(rd.getPublishers())) {
            rd.setPublishers(new RulesDeploy.PublisherType[]{RulesDeploy.PublisherType.RESTFUL});
        }
        return new ByteArrayInputStream(rd.toBytes());
    }

    public String makePathToTheGeneratedFile(String path) {
        return DEF_JAVA_CLASS_PATH + "/" + path;
    }
}
