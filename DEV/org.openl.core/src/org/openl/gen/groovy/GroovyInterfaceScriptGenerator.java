package org.openl.gen.groovy;

import static org.openl.gen.groovy.TypeHelper.DEFAULT_IMPORTS;
import static org.openl.gen.groovy.TypeHelper.PRIMITIVES;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.gen.AnnotationDescription;
import org.openl.gen.MethodDescription;
import org.openl.gen.TypeDescription;

public class GroovyInterfaceScriptGenerator {

    private final String packageName;
    private final String name;
    private final Set<String> imports;
    private final ChainedGroovyScriptWriter writerChain;

    public GroovyInterfaceScriptGenerator(String nameWithPackage, List<MethodDescription> methods) {
        int lastDot = nameWithPackage.lastIndexOf(".");
        String[] dividedName = { nameWithPackage.substring(0, lastDot), nameWithPackage.substring(lastDot + 1) };
        this.packageName = dividedName[0];
        this.name = dividedName[1];
        this.imports = collectImports(methods);
        if (methods != null) {
            ChainedGroovyScriptWriter writerChain = null;
            for (MethodDescription description : methods) {
                writerChain = new GroovyMethodWriter(description, writerChain);
            }
            this.writerChain = writerChain;
        } else {
            this.writerChain = null;
        }
    }

    private Set<String> collectImports(List<MethodDescription> methods) {
        Set<String> result = new HashSet<>();
        if (methods == null) {
            return result;
        }
        for (MethodDescription method : methods) {
            AnnotationDescription[] annotations = method.getAnnotations();
            for (AnnotationDescription annotation : annotations) {
                TypeDescription annotationType = annotation.getAnnotationType();
                addImport(result, annotationType);
            }
            TypeDescription returnType = method.getReturnType();
            addImport(result, returnType);
            for (AnnotationDescription annotation : returnType.getAnnotations()) {
                TypeDescription annotationType = annotation.getAnnotationType();
                addImport(result, annotationType);
            }
            TypeDescription[] argsTypes = method.getArgsTypes();
            for (TypeDescription argsType : argsTypes) {
                AnnotationDescription[] argsAnnotations = argsType.getAnnotations();
                for (AnnotationDescription argsAnnotation : argsAnnotations) {
                    addImport(result, argsAnnotation.getAnnotationType());
                }
                addImport(result, argsType);
            }
        }
        return result;
    }

    private void addImport(Set<String> result, TypeDescription annotationType) {
        final String val = generateImportName(annotationType);
        if (val != null) {
            result.add(val);
        }
    }

    private String generateImportName(TypeDescription annotationType) {
        String simpleName = TypeHelper.removeArrayBrackets(annotationType.getCanonicalName());
        if (DEFAULT_IMPORTS.contains(simpleName) || PRIMITIVES.contains(simpleName)) {
            return null;
        }
        return simpleName;
    }

    private String writeInterface() {
        StringBuilder s = new StringBuilder("");
        s.append("package")
            .append(" ")
            .append(packageName)
            .append(GroovyMethodWriter.LINE_SEPARATOR)
            .append(GroovyMethodWriter.LINE_SEPARATOR);

        for (String anImport : imports) {
            s.append("import").append(" ").append(anImport);
            s.append(GroovyMethodWriter.LINE_SEPARATOR);
        }

        s.append(GroovyMethodWriter.LINE_SEPARATOR);
        s.append("interface")
            .append(" ")
            .append(name)
            .append(" ")
            .append("{")
            .append(GroovyMethodWriter.LINE_SEPARATOR);
        if (writerChain != null) {
            writerChain.write(s, true, imports);
        }

        s.append("}");
        return s.toString();
    }

    public String generatedText() {
        return writeInterface();
    }

}
