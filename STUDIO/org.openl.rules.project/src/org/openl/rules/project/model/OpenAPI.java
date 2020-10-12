package org.openl.rules.project.model;

public final class OpenAPI {
    private String path;

    private String modelModuleName;
    private String algorithmModuleName;

    public OpenAPI() {
    }

    public OpenAPI(String path, String modelModuleName, String algorithmModuleName) {
        this.path = path;
        this.modelModuleName = modelModuleName;
        this.algorithmModuleName = algorithmModuleName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getModelModuleName() {
        return modelModuleName;
    }

    public void setModelModuleName(String modelModuleName) {
        this.modelModuleName = modelModuleName;
    }

    public String getAlgorithmModuleName() {
        return algorithmModuleName;
    }

    public void setAlgorithmModuleName(String algorithmModuleName) {
        this.algorithmModuleName = algorithmModuleName;
    }
}
