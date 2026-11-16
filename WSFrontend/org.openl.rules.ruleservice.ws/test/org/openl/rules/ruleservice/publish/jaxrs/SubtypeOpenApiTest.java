package org.openl.rules.ruleservice.publish.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.openapi.SubtypeReferences;
import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;

/**
 * A reference to a datatype with subtypes lists the subtypes when no type property tells them apart.
 */
class SubtypeOpenApiTest {

    private static final String PARENT = ref("Parent");
    private static final String CHILD = ref("Child");
    private static final String GRAND_CHILD = ref("GrandChild");
    private static final String OTHER = ref("Other");
    private static final String DEFAULT = "default";

    public static class Parent {
        public @Nullable String param1;
    }

    public static class Child extends Parent {
        public @Nullable String param2;
    }

    public static class GrandChild extends Child {
        public @Nullable String param3;
    }

    public static class Other {
        public @Nullable String value;
    }

    public static class Holder {
        public @Nullable String someID;
        public @Nullable Parent myField;
        public Parent @Nullable [] list;
        public @Nullable Map<String, Parent> map;
        public @Nullable Child child;
        public @Nullable GrandChild grandChild;
        public @Nullable Other other;
    }

    /** The JSON views of {@link ViewService}. */
    public static class Public {
    }

    public static class Internal {
    }

    /** An annotation template that returns a datatype for two JSON views. */
    @SuppressWarnings("unused")
    public interface ViewService {
        @GET
        @Path("/public")
        @JsonView(Public.class)
        Holder publicHolder();

        @GET
        @Path("/internal")
        @JsonView(Internal.class)
        Holder internalHolder();
    }

    /** A datatype that requests hold only. */
    public static class Request {
        public @Nullable Parent value;
    }

    /** An annotation template of a service that reads and returns the datatypes. */
    @SuppressWarnings("unused")
    public interface Service {
        @GET
        @Path("/holder")
        Holder holder();

        // A datatype that requests and responses hold both
        @POST
        @Path("/holder")
        Holder holder(Holder holder);

        @POST
        @Path("/parent")
        Parent parent(Parent parent);

        @POST
        @Path("/request")
        String request(Request request);
    }

    @Test
    void listsTheSubtypesWithoutTypeInfo() throws Exception {
        var openAPI = generate(DefaultTypingMode.DISABLED, JsonTypeInfo.Id.CLASS);

        var holder = properties(openAPI, "Holder");
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(holder.get("myField")));
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(holder.get("list").getItems()));
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD),
                anyOf((Schema<?>) holder.get("map").getAdditionalProperties()));
        assertEquals(List.of(CHILD, GRAND_CHILD), anyOf(holder.get("child")));
        // A type without subtypes and a type that has no own subtypes keep the reference
        assertEquals(GRAND_CHILD, holder.get("grandChild").get$ref());
        assertEquals(OTHER, holder.get("other").get$ref());

        // A value is read as the declared type: a request body and a field that requests hold only keep the type
        assertEquals(PARENT, request(openAPI, "/parent").get$ref());
        assertEquals(PARENT, properties(openAPI, "Request").get("value").get$ref());
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(response(openAPI, "/parent")));

        // The schemas of the types stay as they are: a subtype extends its type
        var child = openAPI.getComponents().getSchemas().get("Child");
        assertEquals(PARENT, ((Schema<?>) child.getAllOf().getFirst()).get$ref());
        assertNull(openAPI.getComponents().getSchemas().get("Parent").getAnyOf());
    }

    /** A value read is deduced from its fields, so a request may hold a value of a subtype. */
    @Test
    void listsTheSubtypesOfRequestsWithDeducedTypes() throws Exception {
        var openAPI = generate(DefaultTypingMode.JAVA_LANG_OBJECT, JsonTypeInfo.Id.DEDUCTION);

        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(request(openAPI, "/parent")));
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(properties(openAPI, "Request").get("value")));
        assertEquals(List.of(PARENT, CHILD, GRAND_CHILD), anyOf(response(openAPI, "/parent")));
    }

    @Test
    void keepsTheReferenceWithTypeInfo() throws Exception {
        var openAPI = generate(DefaultTypingMode.JAVA_LANG_OBJECT, JsonTypeInfo.Id.CLASS);

        var holder = properties(openAPI, "Holder");
        assertEquals(PARENT, holder.get("myField").get$ref());
        assertEquals(PARENT, holder.get("list").getItems().get$ref());
        assertEquals(CHILD, holder.get("child").get$ref());
        assertEquals(PARENT, response(openAPI, "/parent").get$ref());
    }

    @Test
    void readsTheDeclaredTypes() throws Exception {
        var openAPI = generate(DefaultTypingMode.JAVA_LANG_OBJECT, JsonTypeInfo.Id.DEDUCTION);

        SubtypeReferences.toDeclaredTypes(openAPI);

        var holder = properties(openAPI, "Holder");
        assertEquals(PARENT, holder.get("myField").get$ref());
        assertEquals(PARENT, holder.get("list").getItems().get$ref());
        assertEquals(PARENT, ((Schema<?>) holder.get("map").getAdditionalProperties()).get$ref());
        assertEquals(CHILD, holder.get("child").get$ref());
        assertEquals(PARENT, request(openAPI, "/parent").get$ref());
        assertEquals(PARENT, response(openAPI, "/parent").get$ref());
    }

    /** A list of types that do not extend the first one is a union of its own, not a type with its subtypes. */
    @Test
    void keepsAUnionOfOtherTypes() {
        var union = new ComposedSchema();
        union.addAnyOfItem(new Schema<>().$ref(PARENT));
        union.addAnyOfItem(new Schema<>().$ref(OTHER));
        var unknown = new ComposedSchema();
        unknown.addAnyOfItem(new Schema<>().$ref(PARENT));
        unknown.addAnyOfItem(new Schema<>().$ref(ref("Unknown")));
        var inline = new ComposedSchema();
        inline.addAnyOfItem(new Schema<>().type("string"));
        // A nullable reference written as a list of one type, and a list that says more about the value
        var single = new ComposedSchema().nullable(true);
        single.addAnyOfItem(new Schema<>().$ref(PARENT));
        var described = typeWithSubtypes().description("Insured");
        var closed = typeWithSubtypes();
        closed.setAdditionalProperties(false);
        var required = typeWithSubtypes();
        required.addRequiredItem("param1");
        var holder = new Schema<>().addProperty("union", union)
                .addProperty("unknown", unknown)
                .addProperty("inline", inline)
                .addProperty("single", single)
                .addProperty("described", described)
                .addProperty("closed", closed)
                .addProperty("required", required);
        var openAPI = openAPI(holder);

        SubtypeReferences.toDeclaredTypes(openAPI);

        assertSame(union, holder.getProperties().get("union"));
        assertSame(unknown, holder.getProperties().get("unknown"));
        assertSame(inline, holder.getProperties().get("inline"));
        assertSame(single, holder.getProperties().get("single"));
        assertSame(described, holder.getProperties().get("described"));
        assertSame(closed, holder.getProperties().get("closed"));
        assertSame(required, holder.getProperties().get("required"));
    }

    /** A type returned for a JSON view lists its subtypes of the same view. */
    @Test
    void listsTheSubtypesOfEachJsonView() throws Exception {
        var factory = new JacksonObjectMapperFactoryBean();
        factory.setDefaultTypingMode(DefaultTypingMode.DISABLED);
        factory.setOverrideClasses(Set.of(Parent.class, Child.class, GrandChild.class, Other.class, Holder.class));
        var openAPI = OpenAPIConfiguration.generateOpenAPI(ViewService.class, factory.createJacksonObjectMapper());

        assertEquals(List.of(ref("Parent_Public"), ref("Child_Public"), ref("GrandChild_Public")),
                anyOf(properties(openAPI, "Holder_Public").get("myField")));
        assertEquals(List.of(ref("Parent_Internal"), ref("Child_Internal"), ref("GrandChild_Internal")),
                anyOf(properties(openAPI, "Holder_Internal").get("myField")));
    }

    /** A list of a type and its subtypes reads as the type in every place a value is described. */
    @Test
    void readsTheDeclaredTypesOutsideOperations() {
        var openAPI = openAPI(new Schema<>());
        var components = openAPI.getComponents();
        components.addRequestBodies("Body",
                new RequestBody().content(new Content().addMediaType(MediaType.APPLICATION_JSON,
                        new io.swagger.v3.oas.models.media.MediaType().schema(typeWithSubtypes()))));
        var pathParameter = new Parameter().name("parent").in("query").schema(typeWithSubtypes());
        openAPI.path("/parent", new PathItem().addParametersItem(pathParameter));

        SubtypeReferences.toDeclaredTypes(openAPI);

        var body = components.getRequestBodies().get("Body").getContent().get(MediaType.APPLICATION_JSON);
        assertEquals(PARENT, body.getSchema().get$ref());
        assertEquals(PARENT, pathParameter.getSchema().get$ref());
    }

    private static ComposedSchema typeWithSubtypes() {
        var schema = new ComposedSchema();
        schema.addAnyOfItem(new Schema<>().$ref(PARENT));
        schema.addAnyOfItem(new Schema<>().$ref(CHILD));
        return schema;
    }

    private static OpenAPI openAPI(Schema<?> holder) {
        var child = new ComposedSchema();
        child.addAllOfItem(new Schema<>().$ref(PARENT));
        return new OpenAPI().components(new Components().addSchemas("Holder", holder)
                .addSchemas("Parent", new Schema<>().addProperty("param1", new Schema<>().type("string")))
                .addSchemas("Child", child)
                .addSchemas("Other", new Schema<>().addProperty("value", new Schema<>().type("string"))));
    }

    private static OpenAPI generate(DefaultTypingMode typingMode, JsonTypeInfo.Id typeInfo)
            throws ClassNotFoundException {
        var factory = new JacksonObjectMapperFactoryBean();
        factory.setDefaultTypingMode(typingMode);
        factory.setJsonTypeInfoId(typeInfo);
        factory.setOverrideClasses(Set.of(Parent.class,
                Child.class,
                GrandChild.class,
                Other.class,
                Holder.class,
                Request.class));
        return OpenAPIConfiguration.generateOpenAPI(Service.class, factory.createJacksonObjectMapper());
    }

    private static Schema<?> request(OpenAPI openAPI, String path) {
        return openAPI.getPaths()
                .get(path)
                .getPost()
                .getRequestBody()
                .getContent()
                .get(MediaType.WILDCARD)
                .getSchema();
    }

    private static Schema<?> response(OpenAPI openAPI, String path) {
        return openAPI.getPaths()
                .get(path)
                .getPost()
                .getResponses()
                .get(DEFAULT)
                .getContent()
                .get(MediaType.WILDCARD)
                .getSchema();
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> properties(OpenAPI openAPI, String name) {
        return openAPI.getComponents().getSchemas().get(name).getProperties();
    }

    @SuppressWarnings("rawtypes")
    private static List<String> anyOf(Schema<?> schema) {
        List<Schema> anyOf = schema.getAnyOf();
        return anyOf.stream().map(Schema::get$ref).toList();
    }

    private static String ref(String name) {
        return Components.COMPONENTS_SCHEMAS_REF + name;
    }
}
