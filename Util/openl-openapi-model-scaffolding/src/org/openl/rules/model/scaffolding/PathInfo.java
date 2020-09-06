package org.openl.rules.model.scaffolding;

import java.util.Objects;

public class PathInfo {
    private String originalPath;
    private String formattedPath;
    private String operation;
    private String returnType;

    public PathInfo() {
    }

    public PathInfo(String originalPath, String formattedPath, String operation, String returnType) {
        this.originalPath = originalPath;
        this.formattedPath = formattedPath;
        this.operation = operation;
        this.returnType = returnType;
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

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
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
        return Objects.equals(returnType, pathInfo.returnType);
    }

    @Override
    public int hashCode() {
        int result = originalPath != null ? originalPath.hashCode() : 0;
        result = 31 * result + (formattedPath != null ? formattedPath.hashCode() : 0);
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        return result;
    }
}
