package org.openl.gen.groovy;

import static org.openl.gen.groovy.TypeHelper.DEFAULT_IMPORTS;

import java.util.Objects;
import java.util.Set;

import org.openl.gen.AnnotationDescription;
import org.openl.gen.MethodDescription;
import org.openl.gen.TypeDescription;

public class GroovyMethodWriter extends ChainedGroovyScriptWriter {

    public static final String RETURN = "return";
    public static final String TAB = "\t";
    private final MethodDescription description;
    public static final String ARG_NAME = "arg";
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public GroovyMethodWriter(MethodDescription description, GroovyWriter next) {
        super(next);
        this.description = Objects.requireNonNull(description, "Method description is null.");
    }

    @Override
    protected void writeInternal(StringBuilder scriptText, boolean isAbstract, Set<String> imports) {
        AnnotationDescription[] annotations = description.getAnnotations();
        for (AnnotationDescription annotation : annotations) {
            scriptText.append(TAB);
            scriptText.append(AnnotationTransformationHelper.transformAnnotation(annotation, '\t', imports));
            scriptText.append(LINE_SEPARATOR);
        }
        scriptText.append(TAB);

        TypeDescription returnType = description.getReturnType();
        String canonicalName = returnType.getCanonicalName();
        scriptText.append(changeCanonicalNameIfImported(imports, canonicalName));
        AnnotationDescription[] returnTypeAnnotations = returnType.getAnnotations();
        for (AnnotationDescription returnTypeAnnotation : returnTypeAnnotations) {
            scriptText.append(AnnotationTransformationHelper.transformAnnotation(returnTypeAnnotation, null, imports));
        }
        scriptText.append(" ");
        String methodName = description.getName();
        scriptText.append(methodName).append("(");

        TypeDescription[] argsTypes = description.getArgsTypes();
        if (argsTypes.length > 0) {
            for (int i = 0; i < argsTypes.length; i++) {
                TypeDescription arg = argsTypes[i];
                AnnotationDescription[] argAnnotation = arg.getAnnotations();
                for (AnnotationDescription annotationDescription : argAnnotation) {
                    scriptText.append(
                        AnnotationTransformationHelper.transformAnnotation(annotationDescription, null, imports));
                    scriptText.append(" ");
                }
                scriptText.append(changeCanonicalNameIfImported(imports, arg.getCanonicalName()))
                    .append(" ")
                    .append(ARG_NAME)
                    .append(i);
                if (i < argsTypes.length - 1) {
                    scriptText.append(", ");
                }
            }
        } else {
            scriptText.append(" ");
        }
        scriptText.append(")");

        if (!isAbstract) {
            scriptText.append(" ").append("{");
            scriptText.append(LINE_SEPARATOR);
            scriptText.append(TAB)
                .append(TAB)
                .append(RETURN)
                .append(" ")
                .append(getDefaultValue(description.getReturnType().getTypeName()))
                .append(";");
            scriptText.append(LINE_SEPARATOR);
            scriptText.append(TAB).append("}");
        }
        scriptText.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    }

    private String changeCanonicalNameIfImported(Set<String> imports, String canonicalName) {
        String result;
        final String typeName = TypeHelper.removeArrayBrackets(canonicalName);
        if (imports.contains(typeName) || DEFAULT_IMPORTS.contains(typeName)) {
            result = TypeHelper.makeImported(canonicalName);
        } else {
            result = canonicalName;
        }
        return result;
    }

    private Object getDefaultValue(String type) {
        switch (type) {
            case "void":
                return "";
            case "byte":
            case "short":
            case "int":
            case "char":
                return "0";
            case "boolean":
                return "false";
            case "long":
                return "0L";
            case "float":
                return "0.0f";
            case "double":
                return "0.0";
            default:
                return "null";
        }
    }

}
