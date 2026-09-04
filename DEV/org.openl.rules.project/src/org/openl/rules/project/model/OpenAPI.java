package org.openl.rules.project.model;

import java.util.Objects;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.util.StringUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class OpenAPI {
    public enum Mode {
        RECONCILIATION,
        GENERATION
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public enum Type {
        YAML("openapi.yaml"),
        YML("openapi.yml"),
        JSON("openapi.json");

        @Getter
        private final String defaultFileName;

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

    @Getter
    @Setter
    private String path;

    @Getter
    @Setter
    @XmlElement(name = "model-module-name")
    private String modelModuleName;
    @Getter
    @Setter
    @XmlElement(name = "algorithm-module-name")
    private String algorithmModuleName;

    @Getter
    @Setter
    private Mode mode;

    public OpenAPI() {
    }

    public OpenAPI(String path, Mode mode, String modelModuleName, String algorithmModuleName) {
        this.path = path;
        this.modelModuleName = modelModuleName;
        this.algorithmModuleName = algorithmModuleName;
        this.mode = mode;
    }

    /** Used by parent containers to decide whether the {@code <openapi>} wrapper can be dropped. */
    static boolean isEmpty(OpenAPI o) {
        return o == null
                || (StringUtils.isBlank(o.path) && StringUtils.isBlank(o.modelModuleName)
                && StringUtils.isBlank(o.algorithmModuleName) && o.mode == null);
    }

    /**
     * {@code true} when the {@code <openapi>} block only restates the runtime defaults — RECONCILIATION
     * mode pointing at the standard {@code openapi.yaml}/{@code .yml}/{@code .json} file with no model
     * or algorithm module overrides — so the wrapper can be dropped without changing behaviour.
     */
    static boolean isDefault(OpenAPI o) {
        return o != null && o.mode == Mode.RECONCILIATION && isDefaultPath(o.path);
    }

    private static boolean isDefaultPath(String path) {
        if (path == null) {
            return false;
        }
        for (Type t : Type.values()) {
            if (t.defaultFileName.equals(path)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private void beforeMarshal(Marshaller marshaller) {
        path = StringUtils.trimToNull(path);
        modelModuleName = StringUtils.trimToNull(modelModuleName);
        algorithmModuleName = StringUtils.trimToNull(algorithmModuleName);
    }
}
