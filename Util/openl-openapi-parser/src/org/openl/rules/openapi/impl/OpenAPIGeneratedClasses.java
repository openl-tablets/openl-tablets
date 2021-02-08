package org.openl.rules.openapi.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OpenAPIGeneratedClasses {

    private final GroovyScriptFile annotationGroovyScript;
    private final Set<GroovyScriptFile> groovyCommonClasses;

    public OpenAPIGeneratedClasses(GroovyScriptFile annotationGroovyScript, Set<GroovyScriptFile> groovyScriptFiles) {
        this.annotationGroovyScript = annotationGroovyScript;
        this.groovyCommonClasses = groovyScriptFiles;
    }

    public GroovyScriptFile getAnnotationTemplateGroovyFile() {
        return annotationGroovyScript;
    }

    public Set<GroovyScriptFile> getGroovyCommonClasses() {
        return groovyCommonClasses;
    }

    public boolean hasAnnotationTemplateClass() {
        return annotationGroovyScript != null;
    }

    static final class Builder {

        private GroovyScriptFile groovyScriptFile;
        private final Set<GroovyScriptFile> groovyCommonClasses = new HashSet<>();

        private Builder() {
        }

        public Builder addGroovyCommonScript(GroovyScriptFile groovyScriptFile) {
            if (!groovyCommonClasses.add(groovyScriptFile)) {
                throw new IllegalArgumentException(
                    String.format("Groovy File Script '%s' is duplicated.", groovyScriptFile));
            }
            return this;
        }

        public Builder setGroovyScriptFile(GroovyScriptFile annotationTemplateClass) {
            this.groovyScriptFile = annotationTemplateClass;
            return this;
        }

        public OpenAPIGeneratedClasses build() {
            return new OpenAPIGeneratedClasses(groovyScriptFile, Collections.unmodifiableSet(groovyCommonClasses));
        }

        public static Builder initialize() {
            return new Builder();
        }

    }
}
