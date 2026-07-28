package org.openl.rules.model.scaffolding;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class PathInfo {
    @Getter
    @Setter
    private String originalPath;
    @Getter
    @Setter
    private String formattedPath;
    @Getter
    @Setter
    private String consumes;
    @Getter
    @Setter
    private String produces;
    @Getter
    @Setter
    private Operation operation;
    @Getter
    @Setter
    private TypeInfo returnType;
    @Getter
    @Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathInfo pathInfo)) {
            return false;
        }
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
