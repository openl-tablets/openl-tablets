package org.openl.rules.openapi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.openl.rules.model.scaffolding.VocabularyModel;
import org.openl.rules.openapi.OpenAPIRefResolver;
import org.openl.util.StringUtils;

/**
 * The vocabulary datatypes an OpenAPI declares through {@code enum}.
 *
 * <p>A named schema with an {@code enum} is a vocabulary of its own name. An inline {@code enum} — on a
 * property, a parameter, a request body or a response — is given a name: its {@code title} when the author
 * set one, otherwise the name of the property or the parameter, or the operation it belongs to. The name is
 * written into the schema's {@code title}, so a type read from that schema later names the vocabulary.
 *
 * <p>Inline enums that declare the values of a vocabulary already met are that vocabulary: a specification
 * generated from rules repeats the values of a vocabulary wherever it is used. A named schema keeps its own
 * name even when another one declares the same values.
 *
 * <p>A {@code null} or an empty value of an {@code enum} is left out: a vocabulary table cannot hold it.
 */
final class OpenAPIVocabularies {

    private static final Set<String> BASE_TYPES = Set.of("string", "integer", "number");

    /** What tells one vocabulary from another: its base type and its values. */
    private record Values(String type, List<String> values) {
    }

    private final OpenAPIRefResolver refResolver;
    private final List<VocabularyModel> vocabularies = new ArrayList<>();
    private final Map<Values, VocabularyModel> byValues = new HashMap<>();
    /** The names given to datatypes, which a vocabulary named after a property or a parameter must not take. */
    private final Set<String> takenNames;
    private final Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    private OpenAPIVocabularies(OpenAPIRefResolver refResolver, Set<String> takenNames) {
        this.refResolver = refResolver;
        this.takenNames = takenNames;
    }

    /**
     * Collects the vocabularies of the specification, naming the inline enums on the way.
     *
     * @param openAPI     the specification; the {@code title} of its inline enums is filled in
     * @param refResolver resolves the references to the components of the specification
     * @return the vocabularies, the named ones first
     */
    static List<VocabularyModel> collect(OpenAPI openAPI, OpenAPIRefResolver refResolver) {
        var schemas = OpenLOpenAPIUtils.getSchemas(openAPI);
        var takenNames = schemas.keySet().stream().map(OpenLOpenAPIUtils::normalizeName).collect(Collectors.toCollection(HashSet::new));
        var collector = new OpenAPIVocabularies(refResolver, takenNames);
        schemas.forEach((name, schema) -> {
            if (isVocabulary(schema)) {
                collector.declare(name, schema, false);
            }
        });
        schemas.forEach((name, schema) -> collector.walk(schema, name));
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().values().forEach(collector::walk);
        }
        return Collections.unmodifiableList(collector.vocabularies);
    }

    /** Whether a schema is a vocabulary: a simple type restricted to a list of values. */
    static boolean isVocabulary(Schema<?> schema) {
        return schema != null && schema.get$ref() == null && schema.getType() != null
                && BASE_TYPES.contains(schema.getType()) && !values(schema).isEmpty() && !isDate(schema);
    }

    private static boolean isDate(Schema<?> schema) {
        return "date".equals(schema.getFormat()) || "date-time".equals(schema.getFormat());
    }

    /** The values a vocabulary table holds for the schema, as they are written in the table. */
    private static List<String> values(Schema<?> schema) {
        return schema.getEnum() == null
                ? List.of()
                : schema.getEnum().stream().filter(Objects::nonNull).map(String::valueOf).filter(StringUtils::isNotBlank).toList();
    }

    private void walk(PathItem pathItem) {
        if (pathItem.getParameters() != null) {
            pathItem.getParameters().forEach(this::walk);
        }
        pathItem.readOperations().forEach(this::walk);
    }

    private void walk(Operation operation) {
        var operationName = StringUtils.isBlank(operation.getOperationId()) ? "Value" : operation.getOperationId();
        if (operation.getParameters() != null) {
            operation.getParameters().forEach(this::walk);
        }
        var requestBody = OpenLOpenAPIUtils.resolve(refResolver, operation.getRequestBody(), RequestBody::get$ref);
        if (requestBody != null) {
            walk(requestBody.getContent(), operationName);
        }
        if (operation.getResponses() != null) {
            operation.getResponses().values().forEach(reference -> {
                // A reference to a response the specification does not declare names nothing.
                var response = OpenLOpenAPIUtils.resolve(refResolver, reference, ApiResponse::get$ref);
                if (response != null) {
                    walk(response.getContent(), operationName + "Result");
                }
            });
        }
    }

    private void walk(Parameter reference) {
        var parameter = OpenLOpenAPIUtils.resolve(refResolver, reference, Parameter::get$ref);
        if (parameter != null) {
            walk(parameter.getSchema(), parameter.getName());
            walk(parameter.getContent(), parameter.getName());
        }
    }

    private void walk(Content content, String name) {
        if (content != null) {
            content.values().forEach(mediaType -> walk(mediaType.getSchema(), name));
        }
    }

    /** Names the inline enums under a schema, the way they are met. */
    private void walk(Schema<?> schema, String name) {
        if (schema == null || schema.get$ref() != null || !visited.add(schema)) {
            return;
        }
        if (isVocabulary(schema)) {
            declare(StringUtils.isBlank(schema.getTitle()) ? name : schema.getTitle(), schema, true);
            return;
        }
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((property, propertySchema) -> walk(propertySchema, property));
        }
        if (schema instanceof ArraySchema arraySchema) {
            walk(arraySchema.getItems(), name);
        }
        if (schema instanceof ComposedSchema composedSchema) {
            Stream.of(composedSchema.getAllOf(), composedSchema.getOneOf(), composedSchema.getAnyOf())
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(partSchema -> walk(partSchema, name));
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> valueSchema) {
            walk(valueSchema, name);
        }
    }

    /**
     * Declares the vocabulary a schema stands for and writes its name into the schema's title.
     *
     * @param reuse whether a vocabulary already met with the same values stands for this schema too, and a
     *              taken name gets a number; a named schema is declared as it is
     */
    private void declare(String name, Schema<?> schema, boolean reuse) {
        visited.add(schema);
        var values = new Values(baseClass(schema).getSimpleName(), values(schema));
        var vocabulary = reuse ? byValues.get(values) : null;
        if (vocabulary == null) {
            vocabulary = new VocabularyModel(reuse ? freeName(name) : OpenLOpenAPIUtils.normalizeName(name), values.type(), values.values());
            vocabularies.add(vocabulary);
            byValues.putIfAbsent(values, vocabulary);
            takenNames.add(vocabulary.name());
        }
        schema.setTitle(vocabulary.name());
    }

    private String freeName(String name) {
        var base = OpenLOpenAPIUtils.normalizeName(StringUtils.capitalize(name));
        var candidate = base;
        var i = 2;
        while (takenNames.contains(candidate)) {
            candidate = base + i++;
        }
        return candidate;
    }

    /** The Java class of the values of a vocabulary: the base type, as the datatype table names it by its simple name. */
    static Class<?> baseClass(Schema<?> schema) {
        return switch (schema.getType()) {
            case "integer" -> "int64".equals(schema.getFormat()) ? Long.class : Integer.class;
            case "number" -> "float".equals(schema.getFormat()) ? Float.class : Double.class;
            default -> String.class;
        };
    }
}
