package org.openl.rules.openapi.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OpenAPIGeneratedClasses {

    private final JavaClassFile annotationTemplateClass;
    private final Set<JavaClassFile> commonClasses;

    private OpenAPIGeneratedClasses(JavaClassFile annotationTemplateClass, Set<JavaClassFile> commonClasses) {
        this.annotationTemplateClass = annotationTemplateClass;
        this.commonClasses = commonClasses;
    }

    public JavaClassFile getAnnotationTemplateClass() {
        return annotationTemplateClass;
    }

    public Set<JavaClassFile> getCommonClasses() {
        return commonClasses;
    }

    public boolean hasAnnotationTemplateClass() {
        return annotationTemplateClass != null;
    }

    static final class Builder {

        private JavaClassFile annotationTemplateClass;
        private final Set<JavaClassFile> commonClasses = new HashSet<>();

        private Builder() {
        }

        public Builder setAnnotationTemplateClass(JavaClassFile annotationTemplateClass) {
            this.annotationTemplateClass = annotationTemplateClass;
            return this;
        }

        public Builder addCommonClass(JavaClassFile classFile) {
            if (!commonClasses.add(classFile)) {
                throw new IllegalArgumentException(String.format("Java Class '%s' is duplicated.", classFile));
            }
            return this;
        }

        public OpenAPIGeneratedClasses build() {
            return new OpenAPIGeneratedClasses(annotationTemplateClass, Collections.unmodifiableSet(commonClasses));
        }

        public static Builder initialize() {
            return new Builder();
        }

    }
}
