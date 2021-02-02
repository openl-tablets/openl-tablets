package org.openl.rules.webstudio.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.model.scaffolding.environment.EnvironmentModel;
import org.openl.rules.openapi.impl.OpenAPIGeneratedClasses;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class OpenAPIHelper {

    public static final String DEF_JAVA_CLASS_PATH = "classes";

    public InputStream generateAlgorithmsModule(final List<SpreadsheetModel> spreadsheetModels,
            final List<DataModel> dataModels,
            final EnvironmentModel environmentModel) throws IOException {
        try (ByteArrayOutputStream sos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateAlgorithmsModule(spreadsheetModels, dataModels, sos, environmentModel);
            byte[] sprBytes = sos.toByteArray();
            return new ByteArrayInputStream(sprBytes);
        }
    }

    public InputStream generateDataTypesFile(final Set<DatatypeModel> datatypeModels) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(datatypeModels, bos);
            byte[] dtBytes = bos.toByteArray();
            return new ByteArrayInputStream(dtBytes);
        }
    }

    public ByteArrayInputStream editOrCreateRulesDeploy(final IRulesDeploySerializer serializer,
            final ProjectModel projectModel,
            final OpenAPIGeneratedClasses generated,
            RulesDeploy exitingRulesDeploy) {
        boolean fileExists = exitingRulesDeploy != null;
        RulesDeploy rd = fileExists ? exitingRulesDeploy : new RulesDeploy();
        if (generated.hasAnnotationTemplateClass()) {
            rd.setAnnotationTemplateClassName(generated.getAnnotationTemplateGroovyFile().getNameWithPackage());
        } else {
            if (StringUtils.isNotBlank(rd.getAnnotationTemplateClassName())) {
                rd.setAnnotationTemplateClassName(null);
            }
        }
        rd.setProvideRuntimeContext(projectModel.isRuntimeContextProvided());
        rd.setProvideVariations(projectModel.areVariationsProvided());
        if (CollectionUtils.isEmpty(rd.getPublishers())) {
            rd.setPublishers(new RulesDeploy.PublisherType[] { RulesDeploy.PublisherType.RESTFUL });
        }
        return new ByteArrayInputStream(serializer.serialize(rd).getBytes(StandardCharsets.UTF_8));
    }

    public String makePathToTheGeneratedFile(String path) {
        return DEF_JAVA_CLASS_PATH + "/" + path;
    }
}
