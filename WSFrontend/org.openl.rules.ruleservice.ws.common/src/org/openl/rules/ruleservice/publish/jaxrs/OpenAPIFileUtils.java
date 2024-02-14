package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Arrays;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.util.StringUtils;

public final class OpenAPIFileUtils {

    private OpenAPIFileUtils() {
    }

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";

    private static ProjectResource loadProjectResource(ProjectResourceLoader projectResourceLoader,
                                                       String name) {
        ProjectResource[] projectResources = projectResourceLoader.loadResource(name, false);
        return Arrays.stream(projectResources)
                .findFirst()
                .orElse(null);
    }

    public static OpenAPI loadOpenAPI(
            ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(projectDescriptor, compiledOpenClass);
        ProjectResource projectResource;
        if (projectDescriptor.getOpenapi() != null && StringUtils
                .isNotBlank(projectDescriptor.getOpenapi().getPath())) {
            projectResource = loadProjectResource(projectResourceLoader,
                    projectDescriptor.getOpenapi().getPath());
            if (projectResource == null) {
                return null;
            }
        } else {
            projectResource = loadProjectResource(projectResourceLoader, OPENAPI_JSON);
            if (projectResource == null) {
                projectResource = loadProjectResource(projectResourceLoader, OPENAPI_YAML);
            }
            if (projectResource == null) {
                projectResource = loadProjectResource(projectResourceLoader, OPENAPI_YML);
            }
        }
        if (projectResource != null) {
            OpenAPIParser openApiParser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            return openApiParser.readLocation(projectResource.getUrl().toString(), null, options)
                    .getOpenAPI();
        }
        return null;
    }
}
