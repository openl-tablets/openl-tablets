package org.openl.rules.project.model;

public final class OpenAPI {

    public enum Mode {
        RECONCILIATION,
        GENERATION
    }

    private String path;

    private String modelModuleName;
    private String algorithmModuleName;

    private Mode mode;

    public OpenAPI() {
    }

    public OpenAPI(String path, Mode mode, String modelModuleName, String algorithmModuleName) {
        this.path = path;
        this.modelModuleName = modelModuleName;
        this.algorithmModuleName = algorithmModuleName;
        this.mode = mode;
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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
