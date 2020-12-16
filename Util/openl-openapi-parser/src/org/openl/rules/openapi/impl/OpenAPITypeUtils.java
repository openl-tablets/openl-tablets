package org.openl.rules.openapi.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

public class OpenAPITypeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAPITypeUtils.class);

    public static final String OBJECT = "Object";
    public static final String SCHEMAS_LINK = "#/components/schemas/";
    public static final String DATE = "Date";
    public static final String STRING = "String";
    public static final String FLOAT = "Float";
    public static final String FLOAT_PRIMITIVE = "float";
    public static final String DOUBLE = "Double";
    public static final String DOUBLE_PRIMITIVE = "double";
    public static final String INTEGER = "Integer";
    public static final String INT = "int";
    public static final String BOOLEAN = "Boolean";
    public static final String BOOLEAN_PRIMITIVE = "boolean";
    public static final String LONG = "Long";
    public static final String LONG_PRIMITIVE = "long";
    public static final String BIG_DECIMAL = "BigDecimal";
    public static final String BIG_INTEGER = "BigInteger";

    public static final Pattern ARRAY_MATCHER = Pattern.compile("[\\[\\]]");
    public static final String DATE_CLASS_NAME = Date.class.getName();
    public static final String STRING_CLASS_NAME = String.class.getName();
    public static final String FLOAT_PRIMITIVE_CLASS_NAME = float.class.getName();
    public static final String FLOAT_CLASS_NAME = Float.class.getName();
    public static final String DOUBLE_PRIMITIVE_CLASS_NAME = double.class.getName();
    public static final String DOUBLE_CLASS_NAME = Double.class.getName();
    public static final String BIG_DECIMAL_CLASS_NAME = BigDecimal.class.getName();
    public static final String LONG_PRIMITIVE_CLASS_NAME = long.class.getName();
    public static final String LONG_CLASS_NAME = Long.class.getName();
    public static final String INTEGER_PRIMITIVE_CLASS_NAME = int.class.getName();
    public static final String INTEGER_CLASS_NAME = Integer.class.getName();
    public static final String BIG_INTEGER_CLASS_NAME = BigInteger.class.getName();
    public static final String BOOLEAN_PRIMITIVE_CLASS_NAME = boolean.class.getName();
    public static final String BOOLEAN_CLASS_NAME = Boolean.class.getName();

    private OpenAPITypeUtils() {
    }

    public static TypeInfo extractType(Schema<?> schema, boolean isPrimitive) {
        TypeInfo result = new TypeInfo();
        if (schema.get$ref() != null) {
            String simpleName = getSimpleName(schema.get$ref());
            result.setSimpleName(simpleName);
            result.setJavaName(simpleName);
            result.setIsReference(true);
            return result;
        }
        String schemaType = schema.getType();
        String format = schema.getFormat();
        if ("string".equals(schemaType)) {
            if ("date".equals(format)) {
                return new TypeInfo(DATE_CLASS_NAME, DATE);
            } else if ("date-time".equals(format)) {
                return new TypeInfo(DATE_CLASS_NAME, DATE);
            } else {
                return new TypeInfo(STRING_CLASS_NAME, STRING);
            }
        } else if ("number".equals(schemaType)) {
            if (FLOAT_PRIMITIVE.equals(format)) {
                return isPrimitive ? new TypeInfo(FLOAT_PRIMITIVE_CLASS_NAME, format)
                                   : new TypeInfo(FLOAT_CLASS_NAME, FLOAT);
            } else if (DOUBLE_PRIMITIVE.equals(format)) {
                return isPrimitive ? new TypeInfo(DOUBLE_PRIMITIVE_CLASS_NAME, format)
                                   : new TypeInfo(DOUBLE_CLASS_NAME, DOUBLE);
            } else {
                return new TypeInfo(BIG_DECIMAL_CLASS_NAME, BIG_DECIMAL);
            }
        } else if ("integer".equals(schemaType)) {
            if ("int64".equals(format)) {
                return isPrimitive ? new TypeInfo(LONG_PRIMITIVE_CLASS_NAME, LONG_PRIMITIVE)
                                   : new TypeInfo(LONG_CLASS_NAME, LONG);
            } else if ("int32".equals(format)) {
                return isPrimitive ? new TypeInfo(INTEGER_PRIMITIVE_CLASS_NAME, INT)
                                   : new TypeInfo(INTEGER_CLASS_NAME, INTEGER);
            } else {
                return new TypeInfo(BIG_INTEGER_CLASS_NAME, BIG_INTEGER);
            }
        } else if (BOOLEAN_PRIMITIVE.equals(schemaType)) {
            return isPrimitive ? new TypeInfo(BOOLEAN_PRIMITIVE_CLASS_NAME, schemaType)
                               : new TypeInfo(BOOLEAN_CLASS_NAME, BOOLEAN);
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            TypeInfo type = extractType(arraySchema.getItems(), false);
            String name = type.getSimpleName() + "[]";
            int dim = type.getDimension() + 1;
            if (type.isReference()) {
                return new TypeInfo(name, name, true, dim);
            } else {
                // TODO: validate me
                String className;
                if (dim == 0) {
                    className = type.getJavaName();
                } else if (dim == 1) {
                    className = "[L" + type.getJavaName() + ";";
                } else {
                    className = "[" + type.getJavaName();
                }
                return new TypeInfo(className, name, false, dim);
            }
        } else {
            return new TypeInfo(Object.class.getName(), OBJECT);
        }
    }

    public static String getSimpleName(String ref) {
        if (ref.startsWith("#/components/")) {
            ref = ref.substring(ref.lastIndexOf('/') + 1);
        } else {
            throw new IllegalStateException(String.format("Invalid ref %s", ref));
        }
        return ref;
    }

    public static boolean isSimpleType(String type) {
        return STRING.equals(type) || FLOAT.equals(type) || DOUBLE.equals(type) || INTEGER.equals(type) || LONG
            .equals(type) || BOOLEAN.equals(type) || DATE.equals(type) || OBJECT
                .equals(type) || BIG_INTEGER.equals(type) || BIG_DECIMAL.equals(type) || isPrimitiveType(type);
    }

    public static boolean isPrimitiveType(String type) {
        return FLOAT_PRIMITIVE.equals(type) || BOOLEAN_PRIMITIVE.equals(type) || INT.equals(type) || LONG_PRIMITIVE
            .equals(type) || DOUBLE_PRIMITIVE.equals(type);
    }

    public static String getSimpleValue(String type) {
        String result;
        switch (type) {
            case INTEGER:
            case INT:
                result = "=0";
                break;
            case BIG_INTEGER:
                result = "=java.math.BigInteger.ZERO";
                break;
            case LONG:
            case LONG_PRIMITIVE:
                result = "=0L";
                break;
            case DOUBLE:
            case DOUBLE_PRIMITIVE:
                result = "=0.0";
                break;
            case FLOAT:
            case FLOAT_PRIMITIVE:
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
            case BOOLEAN_PRIMITIVE:
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
                    sc.getProperties().forEach(propMap::putIfAbsent);
                }
            }
        }
        return propMap;
    }

    public static Map<String, Schema> getAllProperties(ComposedSchema cs, OpenAPI openAPI) {
        Map<String, Schema> allProperties = new HashMap<>(getFieldsOfChild(cs));
        String parentName = getParentName(cs, openAPI);
        Schema<?> parentSchema = OpenLOpenAPIUtils.getSchemas(openAPI).get(parentName);
        if (parentSchema != null) {
            Map<String, Schema> properties = parentSchema.getProperties();
            if (CollectionUtils.isNotEmpty(properties)) {
                properties.forEach(allProperties::putIfAbsent);
            }
            if (parentSchema instanceof ComposedSchema) {
                getAllProperties((ComposedSchema) parentSchema, openAPI).forEach(allProperties::putIfAbsent);
            }
        }
        if (cs != null) {
            Map<String, Schema> properties = cs.getProperties();
            if (CollectionUtils.isNotEmpty(properties)) {
                properties.forEach(allProperties::putIfAbsent);
            }
        }
        return allProperties;
    }

    public static String removeArrayBrackets(String type) {
        return ARRAY_MATCHER.matcher(type).replaceAll("");
    }

    private static boolean isComposedSchema(Schema<?> schema) {
        return schema instanceof ComposedSchema;
    }

}
