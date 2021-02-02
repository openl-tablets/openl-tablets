package org.openl.gen.groovy;

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.openl.gen.AnnotationDescription;
import org.openl.gen.MethodDescription;
import org.openl.gen.TypeDescription;

public class GroovyMethodWriter extends ChainedGroovyScriptWriter {

    public static final String RETURN = "return";
    private final MethodDescription description;
    public static final String ARG_NAME = "arg";
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public GroovyMethodWriter(MethodDescription description, GroovyWriter next) {
        super(next);
        this.description = Objects.requireNonNull(description, "Method description is null.");
    }

    @Override
    protected void writeInternal(StringBuilder scriptText, boolean isAbstract) {
        AnnotationDescription[] annotations = description.getAnnotations();
        for (AnnotationDescription annotation : annotations) {
            scriptText.append("\t");
            scriptText.append(AnnotationTransformationHelper.transformAnnotation(annotation, '\t'));
            scriptText.append(LINE_SEPARATOR);
        }
        scriptText.append("\t");

        TypeDescription returnType = description.getReturnType();
        scriptText.append(returnType.getCanonicalName());
        AnnotationDescription[] returnTypeAnnotations = returnType.getAnnotations();
        for (AnnotationDescription returnTypeAnnotation : returnTypeAnnotations) {
            scriptText.append(AnnotationTransformationHelper.transformAnnotation(returnTypeAnnotation, null));
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
                    scriptText.append(AnnotationTransformationHelper.transformAnnotation(annotationDescription, null));
                    scriptText.append(" ");
                }
                scriptText.append(arg.getCanonicalName()).append(" ").append(ARG_NAME).append(i);
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
            scriptText.append("\t")
                .append("\t")
                .append(RETURN)
                .append(" ")
                .append(getDefaultValue(description.getReturnType().getTypeName()))
                .append(";");
            scriptText.append(LINE_SEPARATOR);
            scriptText.append("\t").append("}");
        }
        scriptText.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
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
