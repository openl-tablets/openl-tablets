package org.openl.rules.openapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;


import static org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter.SPREADSHEET_RESULT;

public class OpenAPITypeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPITypeUtils.class);

    public static final String OBJECT = "Object";
    public static final String SCHEMAS_LINK = "#/components/schemas/";
    public static final String DATE = "Date";
    public static final String STRING = "String";
    public static final String FLOAT = "Float";
    public static final String DOUBLE = "Double";
    public static final String INTEGER = "Integer";
    public static final String BOOLEAN = "Boolean";
    public static final String LONG = "Long";
    public static final String BIG_DECIMAL = "BigDecimal";
    public static final String BIG_INTEGER = "BigInteger";

    private OpenAPITypeUtils() {
    }

    public static String extractType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return getSimpleName(schema.get$ref());
        }
        String schemaType = schema.getType();
        if ("string".equals(schemaType)) {
            if ("date".equals(schema.getFormat())) {
                return DATE;
            } else if ("date-time".equals(schema.getFormat())) {
                return DATE;
            } else {
                return STRING;
            }
        } else if ("number".equals(schemaType)) {
            if ("float".equals(schema.getFormat())) {
                return FLOAT;
            } else if ("double".equals(schema.getFormat())) {
                return DOUBLE;
            } else {
                return BIG_DECIMAL;
            }
        } else if ("integer".equals(schemaType)) {
            if ("int64".equals(schema.getFormat())) {
                return LONG;
            } else if ("int32".equals(schema.getFormat())) {
                return INTEGER;
            } else {
                return BIG_INTEGER;
            }
        } else if ("boolean".equals(schemaType)) {
            return BOOLEAN;
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            String type = extractType(arraySchema.getItems());
            return type != null ? type + "[]" : "";
        } else {
            return OBJECT;
        }
    }

    public static String getSimpleName(String ref) {
        if (ref.startsWith("#/components/")) {
            ref = ref.substring(ref.lastIndexOf('/') + 1);
        } else {
            LOGGER.warn("Failed to get the schema name: {}", ref);
            return null;
        }
        return ref;
    }

    public static boolean isSimpleType(String type) {
        return STRING.equals(type) || FLOAT.equals(type) || DOUBLE.equals(type) || INTEGER.equals(type) || LONG
            .equals(type) || BOOLEAN.equals(type) || DATE
                .equals(type) || OBJECT.equals(type) || BIG_INTEGER.equals(type) || BIG_DECIMAL.equals(type);
    }

    public static String getSimpleValue(String type) {
        String result;
        switch (type) {
            case INTEGER:
                result = "=0";
                break;
            case BIG_INTEGER:
                result = "=java.math.BigInteger.ZERO";
                break;
            case LONG:
                result = "=0L";
                break;
            case DOUBLE:
                result = "=0.0";
                break;
            case FLOAT:
                result = "=0.0f";
                break;
            case BIG_DECIMAL:
                result = "=java.math.BigDecimal.ZERO";
                break;
            case STRING:
                result = "=" + "\"\"";
                break;
            case DATE:
                result = "=new Date()";
                break;
            case BOOLEAN:
                result = "=false";
                break;
            default:
                result = "=new Object()";
                break;
        }
        return result;
    }

    public static String getParentName(ComposedSchema composedSchema, OpenAPI openAPI) {
        Map<String, Schema> allSchemas = OpenLOpenAPIUtils.getSchemas(openAPI);
        List<Schema> interfaces = OpenLOpenAPIUtils.getInterfaces(composedSchema);
        int nullSchemaChildrenCount = 0;
        boolean hasAmbiguousParents = false;
        List<String> refedWithoutDiscriminator = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (Schema<?> schema : interfaces) {
                // get the actual schema
                if (StringUtils.isNotEmpty(schema.get$ref())) {
                    String parentName = OpenAPITypeUtils.getSimpleName(schema.get$ref());
                    Schema<?> s = allSchemas.get(parentName);
                    if (s == null) {
                        LOGGER.error("Failed to obtain schema from {}", parentName);
                        return "UNKNOWN_PARENT_NAME";
                    } else if (hasOrInheritsDiscriminator(s, allSchemas)) {
                        // discriminator.propertyName is used
                        return parentName;
                    } else {
                        // not a parent since discriminator.propertyName is not set
                        hasAmbiguousParents = true;
                        refedWithoutDiscriminator.add(parentName);
                    }
                } else {
                    // not a ref, doing nothing, except counting the number of times the 'null' type
                    // is listed as composed element.
                    if (isNullType(schema)) {
                        // If there are two interfaces, and one of them is the 'null' type,
                        // then the parent is obvious and there is no need to warn about specifying
                        // a determinator.
                        nullSchemaChildrenCount++;
                    }
                }
            }
            if (refedWithoutDiscriminator.size() == 1 && nullSchemaChildrenCount == 1) {
                // One schema is a $ref, and the other is the 'null' type, so the parent is obvious.
                // In this particular case there is no need to specify a discriminator.
                hasAmbiguousParents = false;
            }
        }

        // parent name only makes sense when there is a single obvious parent
        if (refedWithoutDiscriminator.size() == 1) {
            if (hasAmbiguousParents) {
                LOGGER.warn(
                    "Deprecated inheritance without use of 'discriminator.propertyName. Model name: {}. Title: {}",
                    composedSchema.getName(),
                    composedSchema.getTitle());
            }
            return refedWithoutDiscriminator.get(0);
        }

        return null;
    }

    private static boolean hasOrInheritsDiscriminator(Schema<?> schema, Map<String, Schema> allSchemas) {
        if (schema.getDiscriminator() != null && StringUtils.isNotEmpty(schema.getDiscriminator().getPropertyName())) {
            return true;
        } else if (StringUtils.isNotEmpty(schema.get$ref())) {
            String parentName = OpenAPITypeUtils.getSimpleName(schema.get$ref());
            Schema<?> s = allSchemas.get(parentName);
            if (s != null) {
                return hasOrInheritsDiscriminator(s, allSchemas);
            } else {
                LOGGER.error("Failed to obtain schema from {}", parentName);
            }
        } else if (schema instanceof ComposedSchema) {
            final ComposedSchema composed = (ComposedSchema) schema;
            final List<Schema> interfaces = OpenLOpenAPIUtils.getInterfaces(composed);
            for (Schema<?> i : interfaces) {
                if (hasOrInheritsDiscriminator(i, allSchemas)) {
                    return true;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    private static boolean isNullType(Schema<?> schema) {
        return "null".equals(schema.getType());
    }

    public static Map<String, List<String>> getChildrenMap(OpenAPI openAPI) {
        Map<String, Schema> allSchemas = OpenLOpenAPIUtils.getSchemas(openAPI);
        Map<String, List<Map.Entry<String, Schema>>> groupedByParent = allSchemas.entrySet()
            .stream()
            .filter(entry -> isComposedSchema(entry.getValue()))
            .filter(entry -> OpenAPITypeUtils.getParentName((ComposedSchema) entry.getValue(), openAPI) != null)
            .collect(Collectors
                .groupingBy(entry -> OpenAPITypeUtils.getParentName((ComposedSchema) entry.getValue(), openAPI)));

        return groupedByParent.entrySet()
            .stream()
            .collect(Collectors.toMap(mapEntry -> SCHEMAS_LINK + mapEntry.getKey(),
                entry -> entry.getValue().stream().map(x -> SCHEMAS_LINK + x.getKey()).collect(Collectors.toList())));
    }

    public static Map<String, Schema> getFieldsOfChild(ComposedSchema cs) {
        Map<String, Schema> propMap = new HashMap<>();
        List<Schema> interfaces = OpenLOpenAPIUtils.getInterfaces(cs);
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (Schema<?> sc : interfaces) {
                if (StringUtils.isEmpty(sc.get$ref()) && CollectionUtils.isNotEmpty(sc.getProperties())) {
                    propMap.putAll(sc.getProperties());
                }
            }
        }
        return propMap;
    }

    public static boolean isCustomType(String type) {
        return !isSimpleType(type) && !type.equals(SPREADSHEET_RESULT);
    }

    private static boolean isComposedSchema(Schema<?> schema) {
        return schema instanceof ComposedSchema;
    }

}
