package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class PathInfo {
    private String originalPath;
    private String formattedPath;
    private String consumes;
    private String produces;
    private String operation;
    private TypeInfo returnType;
    private InputParameter runtimeContextParameter;

    public PathInfo() {
    }

    public PathInfo(String originalPath, String formattedPath, String operation, TypeInfo typeInfo) {
        this.originalPath = originalPath;
        this.formattedPath = formattedPath;
        this.operation = operation;
        this.returnType = typeInfo;
    }

    public PathInfo(String originalPath,
            String formattedPath,
            String operation,
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
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

        if (!Objects.equals(originalPath, pathInfo.originalPath)) {
            return false;
        }
        if (!Objects.equals(formattedPath, pathInfo.formattedPath)) {
            return false;
        }
        if (!Objects.equals(operation, pathInfo.operation)) {
            return false;
        }
        if (!Objects.equals(consumes, pathInfo.consumes)) {
            return false;
        }
        if (!Objects.equals(produces, pathInfo.produces)) {
            return false;
        }
        if (!Objects.equals(runtimeContextParameter, pathInfo.runtimeContextParameter)) {
            return false;
        }
        return Objects.equals(returnType, pathInfo.returnType);
    }

    @Override
    public int hashCode() {
        int result = originalPath != null ? originalPath.hashCode() : 0;
        result = 31 * result + (formattedPath != null ? formattedPath.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (produces != null ? produces.hashCode() : 0);
        result = 31 * result + (consumes != null ? consumes.hashCode() : 0);
        result = 31 * result + (runtimeContextParameter != null ? runtimeContextParameter.hashCode() : 0);
        return result;
    }
}
