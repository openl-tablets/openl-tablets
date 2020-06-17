package org.open.rules.project.validation.openapi;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.validation.ProjectValidator;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenApiProjectValidator implements ProjectValidator {

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";

    private OpenAPI getOpenAPI(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = getProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_JSON);
        if (projectResource == null) {
            projectResource = getProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YAML);
        }
        if (projectResource == null) {
            projectResource = getProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YML);
        }
        if (projectResource != null) {
            OpenAPIParser openApiParser = new OpenAPIParser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(true);
            return openApiParser.readLocation(new File(projectResource.getUrl().getFile()).getPath(), null, options)
                .getOpenAPI();
        }
        return null;
    }

    private ProjectResource getProjectResource(ProjectResourceLoader projectResourceLoader,
            ProjectDescriptor projectDescriptor,
            String name) {
        ProjectResource[] projectResources = projectResourceLoader.loadResource(name);
        return Arrays.stream(projectResources)
            .filter(e -> Objects.equals(e.getProjectDescriptor().getName(), projectDescriptor.getName()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public CompiledOpenClass validate(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        OpenAPI openAPI = getOpenAPI(projectDescriptor, compiledOpenClass);
        if (openAPI != null) {
            return validateOpenAPI(openAPI, compiledOpenClass);
        }
        return compiledOpenClass;
    }

    private CompiledOpenClass validateOpenAPI(OpenAPI openAPI, CompiledOpenClass compiledOpenClass) {
        ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        return validatedCompiledOpenClass;
    }
}
