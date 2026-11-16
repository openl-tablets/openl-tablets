package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Arrays;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.CompiledOpenClass;
import org.openl.rules.openapi.SubtypeReferences;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenAPIFileUtils {

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";

    private static ProjectResource loadProjectResource(ProjectResourceLoader projectResourceLoader,
                                                       String name) {
        var projectResources = projectResourceLoader.loadResource(name, false);
        return Arrays.stream(projectResources)
                .findFirst()
                .orElse(null);
    }

    public static @Nullable OpenAPI loadOpenAPI(
            ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        var projectResourceLoader = new ProjectResourceLoader(projectDescriptor, compiledOpenClass);
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
            var openApiParser = new OpenAPIParser();
            var options = new ParseOptions();
            options.setResolve(true);
            var openAPI = openApiParser.readLocation(projectResource.getUrl().toString(), null, options)
                    .getOpenAPI();
            if (openAPI != null) {
                // A parameter is described by its declared type, whatever subtypes a value of it may be
                SubtypeReferences.toDeclaredTypes(openAPI);
            }
            return openAPI;
        }
        return null;
    }
}
