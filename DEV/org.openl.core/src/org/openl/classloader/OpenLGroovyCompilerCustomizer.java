package org.openl.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * This class is used by GroovyClassLoader. It is designed to remove annotations from list in all loaded groovy files if
 * they are not found in thread classloader. A list of Annotations to remove is defined in
 * OpenLGroovyCompilerCustomizer.ifMissedIgnoreAnnotations resource located at the same package with this class.
 */
final class OpenLGroovyCompilerCustomizer extends CompilationCustomizer {

    private final Field importsField;

    private final Annotation[] ifMissedIgnoreAnnotations;

    private static class OpenLGroovyCompilerCustomizerHolder {
        private static final OpenLGroovyCompilerCustomizer INSTANCE = new OpenLGroovyCompilerCustomizer(
            CompilePhase.CONVERSION);
    }

    public static OpenLGroovyCompilerCustomizer getInstance() {
        return OpenLGroovyCompilerCustomizerHolder.INSTANCE;
    }

    private static class Annotation {
        String packageName;
        String nameWithoutPackage;

        public Annotation(String packageName, String annotationName) {
            this.packageName = packageName;
            this.nameWithoutPackage = Objects.requireNonNull(annotationName, "nameWithoutPackage cannot be null");
        }
    }

    private OpenLGroovyCompilerCustomizer(CompilePhase phase) {
        super(phase);
        try {
            importsField = ModuleNode.class.getDeclaredField("imports");
            importsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to initialize a field", e);
        }
        Annotation[] tmp = null;
        try (InputStream inputStream = OpenLGroovyCompilerCustomizer.class
            .getResourceAsStream("OpenLGroovyCompilerCustomizer.ifMissedIgnoreAnnotations")) {
            if (inputStream != null) {
                Scanner scanner = new Scanner(inputStream);
                Collection<Annotation> annotations = new ArrayList<>();
                while (scanner.hasNext()) {
                    String className = scanner.nextLine();
                    int p = className.lastIndexOf('.');
                    if (p > 0) {
                        annotations.add(new Annotation(className.substring(0, p), className.substring(p + 1)));
                    } else {
                        annotations.add(new Annotation(null, className));
                    }
                }
                tmp = annotations.toArray(new Annotation[0]);
            }
        } catch (IOException ignored) {
        }
        this.ifMissedIgnoreAnnotations = tmp != null ? tmp : new Annotation[0];
    }

    private Annotation isIfNotFoundThenIgnoreAnnotation(SourceUnit sourceUnit, AnnotationNode annotationNode) {
        for (Annotation annotation : ifMissedIgnoreAnnotations) {
            ClassNode annotationClassNode = annotationNode.getClassNode();
            if (annotationClassNode.getPackageName() == null && Objects.equals(annotation.nameWithoutPackage,
                annotationClassNode.getNameWithoutPackage())) {
                // Import case
                if (sourceUnit.getAST()
                    .getImports()
                    .stream()
                    .anyMatch(e -> Objects.equals(annotation.packageName, e.getType().getPackageName()) && Objects
                        .equals(annotation.nameWithoutPackage, e.getType().getNameWithoutPackage()))) {
                    return annotation;
                }
                // Star imports case
                if (sourceUnit.getAST()
                    .getStarImports()
                    .stream()
                    .anyMatch(e -> Objects.equals(annotation.packageName + ".", e.getPackageName()))) {
                    return annotation;
                }
            }
            // Full class name
            if (annotationClassNode.getPackageName() != null && Objects.equals(annotation.packageName,
                annotationClassNode.getPackageName()) && Objects.equals(annotation.nameWithoutPackage,
                    annotationClassNode.getNameWithoutPackage())) {
                return annotation;
            }
        }
        return null;
    }

    @Override
    public void call(SourceUnit source,
            GeneratorContext context,
            ClassNode classNode) throws CompilationFailedException {
        removeAnnotationsIfNotFoundInClassloader(source, classNode.getAnnotations());
        for (MethodNode methodNode : classNode.getMethods()) {
            removeAnnotationsIfNotFoundInClassloader(source, methodNode.getAnnotations());
            for (Parameter parameter : methodNode.getParameters()) {
                removeAnnotationsIfNotFoundInClassloader(source, parameter.getAnnotations());
            }
        }
        removeImports(source);
    }

    private void removeImports(SourceUnit source) {
        try {
            @SuppressWarnings("unchecked")
            List<ImportNode> imports = (List<ImportNode>) importsField.get(source.getAST());
            imports.removeIf(e -> {
                for (Annotation annotationDescription : ifMissedIgnoreAnnotations) {
                    if (Objects.equals(annotationDescription.packageName, e.getType().getPackageName()) && Objects
                        .equals(annotationDescription.nameWithoutPackage, e.getType().getNameWithoutPackage())) {
                        return true;
                    }
                }
                return false;
            });
        } catch (IllegalAccessException ignored) {
        }
    }

    private void removeAnnotationsIfNotFoundInClassloader(SourceUnit source, List<AnnotationNode> annotationNodes) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Iterator<AnnotationNode> itr = annotationNodes.iterator();
        while (itr.hasNext()) {
            AnnotationNode annotationNode = itr.next();
            Annotation annotation = isIfNotFoundThenIgnoreAnnotation(source, annotationNode);
            if (annotation != null) {
                try {
                    cl.loadClass(annotation.packageName + "." + annotation.nameWithoutPackage);
                } catch (ClassNotFoundException e) {
                    itr.remove();
                }
            }
        }
    }
}
