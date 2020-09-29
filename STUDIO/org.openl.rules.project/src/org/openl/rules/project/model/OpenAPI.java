package org.openl.rules.project.model;

public final class OpenAPI {
    private String path;

    private String modelPath;
    private String algorithmPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getAlgorithmPath() {
        return algorithmPath;
    }

    public void setAlgorithmPath(String algorithmPath) {
        this.algorithmPath = algorithmPath;
    }
}
