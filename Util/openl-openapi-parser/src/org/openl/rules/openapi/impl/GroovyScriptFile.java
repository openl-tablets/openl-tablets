package org.openl.rules.openapi.impl;

import java.util.Objects;

public class GroovyScriptFile {

    private final String nameWithPackage;
    private final String path;
    private final String scriptText;

    public GroovyScriptFile(String nameWithPackage, String scriptText) {
        this.nameWithPackage = Objects.requireNonNull(nameWithPackage, "Groovy Interface name is null.");
        this.path = nameWithPackage.replace('.', '/') + ".groovy";
        this.scriptText = scriptText;
    }

    public String getNameWithPackage() {
        return nameWithPackage;
    }

    public String getPath() {
        return path;
    }

    public String getScriptText() {
        return scriptText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroovyScriptFile that = (GroovyScriptFile) o;
        return Objects.equals(nameWithPackage, that.nameWithPackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameWithPackage);
    }

    @Override
    public String toString() {
        return nameWithPackage;
    }

}
