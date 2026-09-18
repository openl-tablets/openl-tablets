package org.openl.gen;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import org.openl.util.StringUtils;

/**
 * Writes the OpenAPI annotations that describe a property of a generated bean: its description and example, and
 * the values a vocabulary type allows.
 *
 * <p>The annotations are written by name, so the generator needs no OpenAPI package. A class loaded without one
 * on the class path carries them unread.
 *
 * <p>The values of an array of a vocabulary describe its elements, so they go into the {@code @ArraySchema} of
 * the property; a plain {@code @Schema} on an array would restrict the array itself to one of the words. An
 * array of arrays keeps the erased element schema: an annotation describes one level of elements.
 */
public final class OpenApiSchemaAnnotations {

    private static final String SCHEMA = "Lio/swagger/v3/oas/annotations/media/Schema;";
    private static final String ARRAY_SCHEMA = "Lio/swagger/v3/oas/annotations/media/ArraySchema;";

    /** What the annotations are written on: a field or a method of the class being generated. */
    @FunctionalInterface
    public interface Target {
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible);
    }

    private OpenApiSchemaAnnotations() {
    }

    /**
     * Writes the annotations a property needs; none when it has nothing to say.
     *
     * @param target          the field or the method the annotations are written on
     * @param typeDescriptor  the JVM descriptor of the property type
     * @param description     what the property holds, or {@code null}
     * @param example         an example value, or {@code null}
     * @param allowableValues the values a vocabulary type allows, or {@code null} for any other type
     */
    public static void visit(Target target,
                             String typeDescriptor,
                             String description,
                             String example,
                             String[] allowableValues) {
        var hasValues = allowableValues != null && allowableValues.length > 0;
        var hasText = StringUtils.isNotBlank(description) || StringUtils.isNotBlank(example);
        var dimensions = arrayDimensions(typeDescriptor);
        var scalarValues = hasValues && dimensions == 0;
        if (hasValues && dimensions == 1) {
            var arraySchema = target.visitAnnotation(ARRAY_SCHEMA, true);
            if (hasText) {
                var schema = arraySchema.visitAnnotation("arraySchema", SCHEMA);
                visitText(schema, description, example);
                schema.visitEnd();
            }
            var elements = arraySchema.visitAnnotation("schema", SCHEMA);
            visitAllowableValues(elements, allowableValues);
            elements.visitEnd();
            arraySchema.visitEnd();
        } else if (hasText || scalarValues) {
            var schema = target.visitAnnotation(SCHEMA, true);
            visitText(schema, description, example);
            if (scalarValues) {
                visitAllowableValues(schema, allowableValues);
            }
            schema.visitEnd();
        }
    }

    /** Writes the values a vocabulary allows into a {@code @Schema} annotation being written. */
    public static void visitAllowableValues(AnnotationVisitor schema, String[] values) {
        var allowableValues = schema.visitArray("allowableValues");
        for (String value : values) {
            allowableValues.visit(null, value);
        }
        allowableValues.visitEnd();
    }

    private static int arrayDimensions(String typeDescriptor) {
        var type = Type.getType(typeDescriptor);
        return type.getSort() == Type.ARRAY ? type.getDimensions() : 0;
    }

    private static void visitText(AnnotationVisitor schema, String description, String example) {
        if (StringUtils.isNotBlank(description)) {
            schema.visit("description", description);
        }
        if (StringUtils.isNotBlank(example)) {
            schema.visit("example", example);
        }
    }
}
