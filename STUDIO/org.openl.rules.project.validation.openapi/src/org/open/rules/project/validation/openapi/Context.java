package org.open.rules.project.validation.openapi;

import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.types.IOpenClass;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;

class Context {
    private ValidatedCompiledOpenClass validatedCompiledOpenClass;
    private String path;
    private OpenAPI openAPI;
    private PathItem pathItem;
    private Operation operation;
    private ApiResponse apiResponse;
    private IOpenClass openClass;

    public io.swagger.v3.oas.models.OpenAPI getOpenAPI() {
        return openAPI;
    }

    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public ValidatedCompiledOpenClass getValidatedCompiledOpenClass() {
        return validatedCompiledOpenClass;
    }

    public void setValidatedCompiledOpenClass(ValidatedCompiledOpenClass validatedCompiledOpenClass) {
        this.validatedCompiledOpenClass = validatedCompiledOpenClass;
    }

    public PathItem getPathItem() {
        return pathItem;
    }

    public void setPathItem(PathItem pathItem) {
        this.pathItem = pathItem;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(ApiResponse apiResponse) {
        this.apiResponse = apiResponse;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public IOpenClass getOpenClass() {
        return openClass;
    }

    public void setOpenClass(IOpenClass openClass) {
        this.openClass = openClass;
    }
}
