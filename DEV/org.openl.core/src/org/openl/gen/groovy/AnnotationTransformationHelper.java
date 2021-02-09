package org.openl.gen.groovy;

import java.lang.reflect.Array;
import java.util.Set;

import org.objectweb.asm.Type;
import org.openl.gen.AnnotationDescription;
import org.openl.gen.TypeDescription;

public class AnnotationTransformationHelper {

    private AnnotationTransformationHelper() {
    }

    public static String transformAnnotation(AnnotationDescription annotation,
            Character lineOpening,
            Set<String> imports) {
        StringBuilder annotationText;
        if (lineOpening == null) {
            annotationText = new StringBuilder();
        } else {
            annotationText = new StringBuilder(lineOpening);
        }
        TypeDescription annotationType = annotation.getAnnotationType();
        annotationText.append("@");
        String typeName = annotationType.getTypeName();
        if (imports.contains(typeName)) {
            annotationText.append(TypeHelper.makeImported(typeName));
        } else {
            annotationText.append(typeName);
        }
        AnnotationDescription.AnnotationProperty[] properties = annotation.getProperties();
        int propertiesCount = properties.length;
        boolean moreThanOneProperty = propertiesCount > 1;
        for (int j = 0; j < propertiesCount; j++) {
            AnnotationDescription.AnnotationProperty property = properties[j];
            if (j == 0) {
                annotationText.append("(");
            }
            if (moreThanOneProperty) {
                annotationText.append(GroovyMethodWriter.LINE_SEPARATOR);
                annotationText.append(GroovyMethodWriter.TAB).append(GroovyMethodWriter.TAB);
            }
            annotationText.append(property.getName()).append(" ").append("=").append(" ");
            final Object value = property.getValue();

            String readableValue = convertValue(value, imports);
            if (property.isType()) {
                annotationText.append(Type.getType((String) value).getClassName()).append(".class");
            } else if (property.isArray()) {
                if (value.getClass().isArray()) {
                    annotationText.append("[");
                    int length = Array.getLength(value);
                    for (int i = 0; i < length; i++) {
                        final Object elemValue = Array.get(value, i);
                        annotationText.append(convertValue(elemValue, imports));
                        if (i < length - 1) {
                            annotationText.append(",");
                        }
                    }
                    annotationText.append("]");
                } else {
                    annotationText.append("[").append(readableValue).append("]");
                }
            } else {
                annotationText.append(readableValue);
            }
            if (j != propertiesCount - 1) {
                annotationText.append(", ");
            } else {
                if (moreThanOneProperty) {
                    annotationText.append(GroovyMethodWriter.LINE_SEPARATOR);
                    annotationText.append(GroovyMethodWriter.TAB);
                }
                annotationText.append(")");
            }
        }
        return annotationText.toString();
    }

    private static String convertValue(final Object value, final Set<String> imports) {
        String readableValue = value.toString();
        if (value instanceof String) {
            readableValue = "'" + value + "'";
        } else if (value instanceof Enum) {
            String className = value.getClass().getName();
            if (imports.contains(className)) {
                className = TypeHelper.makeImported(className);
            }
            readableValue = className + "." + value;
        }
        return readableValue;
    }
}
