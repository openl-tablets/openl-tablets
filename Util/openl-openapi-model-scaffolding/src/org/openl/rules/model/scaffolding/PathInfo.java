package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class PathInfo {
    private String originalPath;
    private String formattedPath;
    private String consumes;
    private String produces;
    private Operation operation;
    private TypeInfo returnType;
    private InputParameter runtimeContextParameter;

    public PathInfo() {
    }

    public PathInfo(String originalPath, String formattedPath, Operation operation, TypeInfo typeInfo) {
        this.originalPath = originalPath;
        this.formattedPath = formattedPath;
        this.operation = operation;
        this.returnType = typeInfo;
    }

    public PathInfo(String originalPath,
                    String formattedPath,
                    Operation operation,
                    TypeInfo returnType,
                    String consumes,
                    String produces) {
        this.originalPath = originalPath;
        this.formattedPath = formattedPath;
        this.operation = operation;
        this.returnType = returnType;
        this.consumes = consumes;
        this.produces = produces;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getFormattedPath() {
        return formattedPath;
    }

    public void setFormattedPath(String formattedPath) {
        this.formattedPath = formattedPath;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public TypeInfo getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeInfo returnType) {
        this.returnType = returnType;
    }

    public String getConsumes() {
        return consumes;
    }

    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    public InputParameter getRuntimeContextParameter() {
        return runtimeContextParameter;
    }

    public void setRuntimeContextParameter(InputParameter runtimeContextParameter) {
        this.runtimeContextParameter = runtimeContextParameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathInfo pathInfo = (PathInfo) o;
        return Objects.equals(originalPath, pathInfo.originalPath)
                && Objects.equals(formattedPath, pathInfo.formattedPath)
                && Objects.equals(consumes, pathInfo.consumes)
                && Objects.equals(produces, pathInfo.produces)
                && operation == pathInfo.operation
                && Objects.equals(returnType, pathInfo.returnType)
                && Objects.equals(runtimeContextParameter, pathInfo.runtimeContextParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPath, formattedPath, consumes, produces, operation, returnType,
                runtimeContextParameter);
    }

    public enum Operation {

        POST,
        GET,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE

    }
}
