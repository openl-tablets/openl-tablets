package org.openl.rules.project.model;

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class OpenAPI {
    public enum Mode {
        RECONCILIATION,
        GENERATION
    }

    public enum Type {
        YAML("openapi.yaml"),
        YML("openapi.yml"),
        JSON("openapi.json");

        private final String defaultFileName;

        Type(String defaultFileName) {
            this.defaultFileName = defaultFileName;
        }

        public String getDefaultFileName() {
            return defaultFileName;
        }

        public static Type chooseType(String extension) {
            if (Objects.equals("json", extension)) {
                return JSON;
            } else if (Objects.equals("yaml", extension)) {
                return YAML;
            } else if (Objects.equals("yml", extension)) {
                return YML;
            }
            return null;
        }
    }

    private String path;

    @XmlElement(name = "model-module-name")
    private String modelModuleName;
    @XmlElement(name = "algorithm-module-name")
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
