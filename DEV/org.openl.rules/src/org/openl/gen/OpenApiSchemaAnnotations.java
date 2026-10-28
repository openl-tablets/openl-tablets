package org.openl.gen;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
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
 * <p>The description and the example go into a {@code @Parameter} of the property. They describe the property
 * only: a {@code @Schema} would describe the type of the property too, so the text would show on the elements of
 * a list or a map and on the schema of a datatype the property refers to.
 *
 * <p>The values of an array of a vocabulary describe its elements, so they go into the {@code @ArraySchema} of
 * the property; a plain {@code @Schema} on an array would restrict the array itself to one of the words. An
 * array of arrays keeps the erased element schema: an annotation describes one level of elements.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenApiSchemaAnnotations {

    private static final String PARAMETER = "Lio/swagger/v3/oas/annotations/Parameter;";
    private static final String SCHEMA = "Lio/swagger/v3/oas/annotations/media/Schema;";
    private static final String ARRAY_SCHEMA = "Lio/swagger/v3/oas/annotations/media/ArraySchema;";

    /** What the annotations are written on: a field or a method of the class being generated. */
    @FunctionalInterface
    public interface Target {
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible);
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
                             String @Nullable [] allowableValues) {
        if (StringUtils.isNotBlank(description) || StringUtils.isNotBlank(example)) {
            var parameter = target.visitAnnotation(PARAMETER, true);
            visitText(parameter, description, example);
            parameter.visitEnd();
        }
        if (allowableValues == null || allowableValues.length == 0) {
            return;
        }
        switch (arrayDimensions(typeDescriptor)) {
            case 0 -> {
                var schema = target.visitAnnotation(SCHEMA, true);
                visitAllowableValues(schema, allowableValues);
                schema.visitEnd();
            }
            case 1 -> {
                var arraySchema = target.visitAnnotation(ARRAY_SCHEMA, true);
                var elements = arraySchema.visitAnnotation("schema", SCHEMA);
                visitAllowableValues(elements, allowableValues);
                elements.visitEnd();
                arraySchema.visitEnd();
            }
            default -> {
                // an array of arrays keeps the erased element schema
            }
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

    private static void visitText(AnnotationVisitor parameter, String description, String example) {
        if (StringUtils.isNotBlank(description)) {
            parameter.visit("description", description);
        }
        if (StringUtils.isNotBlank(example)) {
            parameter.visit("example", example);
        }
    }
}
