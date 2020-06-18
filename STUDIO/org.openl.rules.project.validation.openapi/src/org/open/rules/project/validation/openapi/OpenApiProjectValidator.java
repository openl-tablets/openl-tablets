package org.open.rules.project.validation.openapi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open.rules.project.validation.openapi.utils.MethodUtils;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.validation.AbstractServiceInterfaceProjectValidator;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.types.IOpenMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenApiProjectValidator extends AbstractServiceInterfaceProjectValidator {
    private final Logger log = LoggerFactory.getLogger(OpenApiProjectValidator.class);

    private static final String OPENAPI_JSON = "openapi.json";
    private static final String OPENAPI_YAML = "openapi.yaml";
    private static final String OPENAPI_YML = "openapi.yml";

    private OpenAPI loadOpenAPI(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_JSON);
        if (projectResource == null) {
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YAML);
        }
        if (projectResource == null) {
            projectResource = loadProjectResource(projectResourceLoader, projectDescriptor, OPENAPI_YML);
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

    @Override
    public CompiledOpenClass validate(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException {
        final CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
        OpenAPI openAPI = loadOpenAPI(projectDescriptor, compiledOpenClass);
        if (openAPI == null) {
            // return compiledOpenClass;
        }
        final Context context = new Context();
        context.setOpenAPI(openAPI);
        ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        context.setValidatedCompiledOpenClass(validatedCompiledOpenClass);
        context.setOpenClass(validatedCompiledOpenClass.getOpenClassWithErrors());
        Class<?> serviceClass = resolveInterface(projectDescriptor,
            rulesInstantiationStrategy,
            validatedCompiledOpenClass);
        if (openAPI != null) {
            return validateOpenAPI(context, compiledOpenClass);
        }
        return compiledOpenClass;
    }

    private CompiledOpenClass validateOpenAPI(Context context, CompiledOpenClass compiledOpenClass) {
        ValidatedCompiledOpenClass validatedCompiledOpenClass = new OpenApiValidatedCompiledOpenClass(
            compiledOpenClass);
        for (Map.Entry<String, PathItem> entry : context.getOpenAPI().getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();
            try {
                context.setPath(entry.getKey());
                context.setPathItem(pathItem);
                context.setOperation(pathItem.getPost() != null ? pathItem.getPost() : pathItem.getGet());
                validateOperation(context);
            } finally {
                context.setPath(null);
                context.setPathItem(null);
                context.setOperation(null);
            }
        }
        return validatedCompiledOpenClass;
    }

    private String extractMethodName(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private IOpenMethod findMethodByString(Context context, String id) {
        List<IOpenMethod> openMethods = new ArrayList<>();
        for (IOpenMethod m : context.getOpenClass().getMethods()) {
            if (Objects.equals(m.getName(), id)) {
                openMethods.add(m);
            }
        }
        if (openMethods.size() == 1) {
            return openMethods.get(0);
        }
        if (openMethods.size() > 1) {
            MethodUtils.sort(openMethods);
            for (IOpenMethod openMethod : openMethods) {

            }
        }
        if (openMethods.size() == 0) {
            return null;
        }
        return null;
    }

    private IOpenMethod findMethod(Context context) {
        if (context.getOperation().getOperationId() != null) {
            IOpenMethod method = findMethodByString(context, context.getOperation().getOperationId());
            if (method != null) {
                return method;
            }
        }
        return findMethodByString(context, extractMethodName(context.getPath()));
    }

    private void validateOperation(Context context) {
        final String path = context.getPath();
        final String methodName = null;
        for (IOpenMethod openMethod : context.getOpenClass().getMethods()) {
            if (Objects.equals(openMethod.getName(), openMethod.getName())) {

            }
        }
        validateRequest(context);
        validateResponse(context);
    }

    private void validateResponse(Context context) {
        final Operation operation = context.getOperation();
        final ApiResponse apiResponse = operation.getResponses().getDefault() != null ? operation.getResponses()
            .getDefault() : operation.getResponses().get("200");

    }

    private void validateRequest(Context context) {
        final RequestBody requestBody = context.getOperation().getRequestBody();

    }

}
