package org.openl.studio.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.extern.slf4j.Slf4j;

/**
 * Best-effort JSON schema generation for run, test and trace API responses.
 * <p>
 * The schema is optional metadata, so a generation failure must degrade to {@code null} rather than
 * failing the whole request. Self-referential generated beans are handled by
 * {@link JacksonBeanSchemaProvider}; this additionally guards against any other failure, including a
 * {@link StackOverflowError} on a pathologically nested type.
 */
@Slf4j
public final class SafeSchemaGenerator {

    private SafeSchemaGenerator() {
    }

    /**
     * Generates a JSON schema for the given class, returning {@code null} instead of throwing on any
     * failure.
     *
     * @param generator the configured schema generator
     * @param clazz     the class to describe; a {@code null} class yields a {@code null} schema
     * @return the generated schema, or {@code null} if it could not be generated
     */
    public static ObjectNode generate(SchemaGenerator generator, Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            return generator.generateSchema(clazz);
        } catch (Exception | StackOverflowError e) {
            log.warn("Failed to generate JSON schema for type '{}'.", clazz.getName(), e);
            return null;
        }
    }
}
