package org.openl.rules.openapi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Lists the subtypes a value of a type may be, where no type property tells them apart.
 *
 * <p>A value of a type that has subtypes may be a value of any of them, with the fields the subtype adds. When the
 * type has a discriminator, the type property of the value names the subtype, and the schema of the type tells it.
 * Without a discriminator, nothing in the reference to the type tells it. So a place that holds a value of such a type
 * refers to the type and all its subtypes instead: {@code anyOf: [Type, Subtype, ...]}. The type comes first, then
 * its subtypes by name. {@code anyOf} is used rather than {@code oneOf}: a value of a subtype matches the schema of the
 * type too.
 *
 * <p>A type that is read without its subtypes, as when the type info is switched off, keeps the reference where a
 * value is only read: a request body, a parameter, a field of a schema that only requests hold. A value sent there
 * is read as the declared type, so the fields of a subtype are not read. Where a value is returned, a field of a
 * schema that responses hold too included, it may be a value of a subtype and lists them.
 *
 * <p>The schemas of the types stay as they are: a subtype still extends its type through {@code allOf}. A reference
 * inside {@code allOf}, {@code anyOf} or {@code oneOf} tells how a schema is composed, not what a value holds, so it
 * is left as it is.
 *
 * <p>A project generated from an OpenAPI and the OpenAPI reconciliation of a project need the declared type of a
 * place. {@link #toDeclaredTypes} turns such a list back into the reference to the type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubtypeReferences {

    private static final String SCHEMAS_REF = Components.COMPONENTS_SCHEMAS_REF;

    /**
     * Refers to the type and all its subtypes wherever the OpenAPI refers to a type without a discriminator.
     *
     * @param openAPI        the OpenAPI to update
     * @param subtypesByName the names of the schemas of the direct subtypes, by the name of the schema of the type
     * @param readAsDeclared the names of the schemas of the types whose values are read without their subtypes
     */
    static void toSubtypes(OpenAPI openAPI, Map<String, List<String>> subtypesByName, Set<String> readAsDeclared) {
        var schemas = schemasOf(openAPI);
        if (schemas != null && !subtypesByName.isEmpty()) {
            new SubtypeLists(schemas, subtypesByName, readAsDeclared, openAPI).walk(openAPI);
        }
    }

    /**
     * Refers to the declared type wherever the OpenAPI refers to a type and its subtypes: an {@code anyOf} of two or
     * more references where every reference after the first one refers to a schema that extends the first one.
     *
     * <p>Any other {@code anyOf} stays as it is, and so does a schema that says more than the list, such as a
     * description or that the value may be null.
     *
     * @param openAPI the OpenAPI to update
     */
    public static void toDeclaredTypes(OpenAPI openAPI) {
        var schemas = schemasOf(openAPI);
        if (schemas != null) {
            new DeclaredTypes(schemas).walk(openAPI);
        }
    }

    @SuppressWarnings("rawtypes")
    private static @Nullable Map<String, Schema> schemasOf(OpenAPI openAPI) {
        return openAPI.getComponents() == null ? null : openAPI.getComponents().getSchemas();
    }

    /** Returns the name of the schema a reference refers to, or {@code null} if it is not such a reference. */
    private static @Nullable String schemaName(Schema<?> schema) {
        var ref = schema.get$ref();
        return ref != null && ref.startsWith(SCHEMAS_REF) ? ref.substring(SCHEMAS_REF.length()) : null;
    }

    /** Returns the names of the schemas a schema extends through {@code allOf}. */
    @SuppressWarnings("rawtypes")
    private static List<String> parentsOf(Schema<?> schema) {
        List<Schema> allOf = schema.getAllOf();
        if (allOf == null) {
            return List.of();
        }
        return allOf.stream().map(SubtypeReferences::schemaName).filter(Objects::nonNull).toList();
    }

    /** Returns the schemas of the responses of the OpenAPI. */
    private static Stream<Schema<?>> responseSchemas(OpenAPI openAPI) {
        var components = openAPI.getComponents().getResponses();
        var responses = Stream.concat(
                components == null ? Stream.empty() : components.values().stream(),
                operations(openAPI).filter(operation -> operation.getResponses() != null)
                        .flatMap(operation -> operation.getResponses().values().stream()));
        return responses.flatMap(response -> mediaTypes(response.getContent()))
                .<Schema<?>>map(MediaType::getSchema)
                .filter(Objects::nonNull);
    }

    private static Stream<Operation> operations(OpenAPI openAPI) {
        return openAPI.getPaths() == null
                ? Stream.empty()
                : openAPI.getPaths().values().stream().flatMap(pathItem -> pathItem.readOperations().stream());
    }

    private static Stream<MediaType> mediaTypes(@Nullable Content content) {
        return content == null ? Stream.empty() : content.values().stream();
    }

    /** Visits every schema of a value in an OpenAPI, and replaces the schemas a value is described by. */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private abstract static class Walker {

        @SuppressWarnings("rawtypes")
        final Map<String, Schema> schemas;
        private final Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        /** Whether the values being visited are returned, or only read. */
        boolean returned;

        /** Returns the schema a value of the given schema is described by. */
        abstract Schema<?> replacement(Schema<?> schema);

        /** Tells whether the values of a schema of the components are returned. */
        abstract boolean returned(String name);

        void walk(OpenAPI openAPI) {
            var components = openAPI.getComponents();
            // A schema of a type is its definition, so it is not replaced, only what it holds is
            schemas.forEach((name, schema) -> {
                returned = returned(name);
                visitSchema(schema);
            });
            returned = false;
            if (components.getParameters() != null) {
                visitParameters(List.copyOf(components.getParameters().values()));
            }
            if (components.getRequestBodies() != null) {
                components.getRequestBodies().values().forEach(body -> visitContent(body.getContent()));
            }
            if (openAPI.getPaths() != null) {
                for (var pathItem : openAPI.getPaths().values()) {
                    visitParameters(pathItem.getParameters());
                    pathItem.readOperations().forEach(this::visitRequest);
                }
            }
            returned = true;
            if (components.getResponses() != null) {
                components.getResponses().values().forEach(response -> visitContent(response.getContent()));
            }
            operations(openAPI).filter(operation -> operation.getResponses() != null)
                    .forEach(operation -> operation.getResponses()
                            .values()
                            .forEach(response -> visitContent(response.getContent())));
        }

        private void visitRequest(Operation operation) {
            visitParameters(operation.getParameters());
            if (operation.getRequestBody() != null) {
                visitContent(operation.getRequestBody().getContent());
            }
        }

        private void visitParameters(@Nullable List<Parameter> parameters) {
            if (parameters != null) {
                for (var parameter : parameters) {
                    parameter.setSchema(replace(parameter.getSchema()));
                    visitContent(parameter.getContent());
                }
            }
        }

        private void visitContent(@Nullable Content content) {
            mediaTypes(content).forEach(mediaType -> mediaType.setSchema(replace(mediaType.getSchema())));
        }

        /** Replaces the schema of a value, then visits what the schema holds. */
        @SuppressWarnings("rawtypes")
        private @Nullable Schema replace(@Nullable Schema schema) {
            if (schema == null) {
                return null;
            }
            var replaced = replacement(schema);
            visitSchema(replaced);
            return replaced;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void visitSchema(Schema<?> schema) {
            if (!visited.add(schema)) {
                return;
            }
            if (schema.getProperties() != null) {
                Map<String, Schema> properties = schema.getProperties();
                properties.replaceAll((name, property) -> replace(property));
            }
            schema.setItems(replace(schema.getItems()));
            if (schema.getAdditionalProperties() instanceof Schema additionalProperties) {
                schema.setAdditionalProperties(replace(additionalProperties));
            }
            visitInline(schema.getAllOf());
            visitInline(schema.getAnyOf());
            visitInline(schema.getOneOf());
        }

        /** Visits the schemas a schema is composed of that are not references. */
        @SuppressWarnings("rawtypes")
        private void visitInline(@Nullable List<Schema> composition) {
            if (composition != null) {
                composition.stream().filter(item -> item.get$ref() == null).forEach(this::visitSchema);
            }
        }
    }

    /** Refers to a type and its subtypes where a value of the type may be a value of a subtype. */
    private static final class SubtypeLists extends Walker {

        private final Map<String, List<String>> subtypesByName;
        private final Set<String> readAsDeclared;
        private final Set<String> returnedSchemas;
        private final Map<String, List<String>> typesByName = new HashMap<>();

        @SuppressWarnings("rawtypes")
        SubtypeLists(Map<String, Schema> schemas,
                     Map<String, List<String>> subtypesByName,
                     Set<String> readAsDeclared,
                     OpenAPI openAPI) {
            super(schemas);
            this.subtypesByName = subtypesByName;
            this.readAsDeclared = readAsDeclared;
            // Before any reference is replaced: the schemas reached from the responses
            this.returnedSchemas = reachedSchemas(responseSchemas(openAPI));
        }

        @Override
        boolean returned(String name) {
            return returnedSchemas.contains(name);
        }

        @Override
        Schema<?> replacement(Schema<?> schema) {
            var name = schemaName(schema);
            if (name == null || !returned && readAsDeclared.contains(name)) {
                return schema;
            }
            var types = typesByName.computeIfAbsent(name, this::typeWithSubtypes);
            if (types.isEmpty()) {
                return schema;
            }
            var anyOf = new ComposedSchema();
            types.forEach(type -> anyOf.addAnyOfItem(new Schema<>().$ref(SCHEMAS_REF + type)));
            return anyOf;
        }

        /**
         * Returns the names of a type and all its subtypes, the type first, or none if the type has no subtypes or a
         * discriminator tells them apart.
         */
        private List<String> typeWithSubtypes(String name) {
            var subtypes = subtypesOf(name);
            if (subtypes.isEmpty() || hasDiscriminator(name, new HashSet<>())) {
                return List.of();
            }
            var types = new ArrayList<String>(subtypes.size() + 1);
            types.add(name);
            types.addAll(subtypes);
            return types;
        }

        /** Returns the names of the schemas of all the subtypes of a type, subtypes of subtypes too, by name. */
        private Set<String> subtypesOf(String name) {
            var subtypes = new TreeSet<String>();
            var pending = new ArrayDeque<>(subtypesByName.getOrDefault(name, List.of()));
            while (!pending.isEmpty()) {
                var subtype = pending.poll();
                if (schemas.containsKey(subtype) && subtypes.add(subtype)) {
                    pending.addAll(subtypesByName.getOrDefault(subtype, List.of()));
                }
            }
            return subtypes;
        }

        /**
         * Returns the names of the schemas the given schemas hold, through their fields and their elements, and
         * the schemas of all the subtypes of those, as a value of a subtype may be held there.
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        private Set<String> reachedSchemas(Stream<Schema<?>> roots) {
            var names = new HashSet<String>();
            var pending = new ArrayDeque<Schema<?>>();
            roots.forEach(pending::add);
            while (!pending.isEmpty()) {
                var schema = pending.poll();
                var name = schemaName(schema);
                if (name != null) {
                    if (names.add(name) && schemas.get(name) != null) {
                        pending.add(schemas.get(name));
                        subtypesOf(name).forEach(subtype -> pending.add(new Schema<>().$ref(SCHEMAS_REF + subtype)));
                    }
                } else {
                    if (schema.getProperties() != null) {
                        pending.addAll((Collection) schema.getProperties().values());
                    }
                    if (schema.getItems() != null) {
                        pending.add(schema.getItems());
                    }
                    if (schema.getAdditionalProperties() instanceof Schema additionalProperties) {
                        pending.add(additionalProperties);
                    }
                    Stream.of(schema.getAllOf(), schema.getAnyOf(), schema.getOneOf())
                            .filter(Objects::nonNull)
                            .forEach(composition -> pending.addAll((List) composition));
                }
            }
            return names;
        }

        /** Tells whether the schema of a type or the schema of any type it extends has a discriminator. */
        private boolean hasDiscriminator(String name, Set<String> seen) {
            var schema = schemas.get(name);
            if (schema == null || !seen.add(name)) {
                return false;
            }
            return schema.getDiscriminator() != null
                    || parentsOf(schema).stream().anyMatch(parent -> hasDiscriminator(parent, seen));
        }
    }

    /** Refers to the declared type where a type and its subtypes are listed. */
    private static final class DeclaredTypes extends Walker {

        @SuppressWarnings("rawtypes")
        DeclaredTypes(Map<String, Schema> schemas) {
            super(schemas);
        }

        @Override
        boolean returned(String name) {
            return true;
        }

        @Override
        @SuppressWarnings("rawtypes")
        Schema<?> replacement(Schema<?> schema) {
            List<Schema> anyOf = schema.getAnyOf();
            if (!holdsTypesOnly(schema) || anyOf.size() < 2) {
                return schema;
            }
            var type = schemaName(anyOf.getFirst());
            if (type == null) {
                return schema;
            }
            var subtypesOnly = anyOf.stream()
                    .skip(1)
                    .map(SubtypeReferences::schemaName)
                    .allMatch(subtype -> subtype != null && extendsType(subtype, type, new HashSet<>()));
            return subtypesOnly ? anyOf.getFirst() : schema;
        }

        /**
         * Tells whether a schema says nothing but the list of the types a value may be. A schema that says more about
         * the value, such as its description, that it may be null or which fields it requires, keeps it.
         */
        @SuppressWarnings("rawtypes")
        private static boolean holdsTypesOnly(Schema<?> schema) {
            if (schema.getAnyOf() == null) {
                return false;
            }
            Schema bare;
            try {
                bare = schema.getClass().getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                return false;
            }
            bare.setAnyOf(schema.getAnyOf());
            bare.setSpecVersion(schema.getSpecVersion());
            return bare.equals(schema);
        }

        /** Tells whether the schema of a type extends the schema of another type, directly or through its parents. */
        private boolean extendsType(String name, String type, Set<String> seen) {
            var schema = schemas.get(name);
            if (schema == null || !seen.add(name)) {
                return false;
            }
            var parents = parentsOf(schema);
            return parents.contains(type) || parents.stream().anyMatch(parent -> extendsType(parent, type, seen));
        }
    }
}
