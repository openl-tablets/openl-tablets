package org.openl.rules.openapi.impl;

import java.util.Objects;

import lombok.Getter;

public class GroovyScriptFile {

    @Getter
    private final String nameWithPackage;
    @Getter
    private final String path;
    @Getter
    private final String scriptText;

    public GroovyScriptFile(String nameWithPackage, String scriptText) {
        this.nameWithPackage = Objects.requireNonNull(nameWithPackage, "Groovy Interface name is null.");
        this.path = nameWithPackage.replace('.', '/') + ".groovy";
        this.scriptText = scriptText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroovyScriptFile that)) {
            return false;
        }
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
