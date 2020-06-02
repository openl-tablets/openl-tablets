package org.openl.rules.webstudio.util.converter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.openl.rules.webstudio.util.ExcelFileBuilder;
import org.openl.rules.webstudio.util.converter.OpenAPIModelConverter;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extracts data types from OpenAPI JSON files
 */
public class JsonToExcelConverter implements OpenAPIModelConverter {

    private static final Logger logger = LoggerFactory.getLogger(JsonToExcelConverter.class);

    public static final String INFO = "info";
    public static final String TITLE = "title";
    public static final String COMPONENTS = "components";
    public static final String SCHEMAS = "schemas";
    public static final String PROPERTIES = "properties";
    public static final String TYPE = "type";
    public static final String DEFAULT_VALUE = "default";
    // #/components/schemas/
    public static final int REF_SCHEMA_LENGTH = 21;

    public static final String REF = "$ref";
    public static final String ARRAY = "array";
    public static final String ITEMS = "items";
    private static final String FORMAT = "format";

    @Override
    public void extractDataTypes(String pathTo) {
        ClassLoader classLoader = JsonToExcelConverter.class.getClassLoader();
        JsonNode content;
        String projectName = "";
        List<DatatypeDto> dataTypes = new ArrayList<>();
        try {
            content = new ObjectMapper().readTree(Objects.requireNonNull(classLoader.getResourceAsStream(pathTo)));
            JsonNode info = content.get(INFO);
            projectName = info.get(TITLE).asText();
            dataTypes = extractDataTypes(content);
        } catch (JsonProcessingException e) {
            logger.error("There is a problem with JSON file format", e);
        } catch (IOException e) {
            logger.error("There is a problem with JSON file", e);
        }
        ExcelFileBuilder.generateExcelFile(projectName, dataTypes);
    }

    private List<DatatypeDto> extractDataTypes(JsonNode content) {
        List<DatatypeDto> result = new ArrayList<>();
        JsonNode schemas = content.get(COMPONENTS).get(SCHEMAS);
        Iterator<String> schemaNamesIterator = schemas.fieldNames();
        while (schemaNamesIterator.hasNext()) {
            String requestName = schemaNamesIterator.next();
            JsonNode requestProperties = schemas.get(requestName).get(PROPERTIES);
            Iterator<String> propertiesIterator = requestProperties.fieldNames();
            DatatypeDto datatype = new DatatypeDto(requestName);
            List<FieldDto> fields = new ArrayList<>();
            while (propertiesIterator.hasNext()) {
                String propertyName = propertiesIterator.next();
                JsonNode propertyNode = requestProperties.get(propertyName);
                String propertyType = extractType(propertyNode);
                JsonNode defaultValueNode = propertyNode.get(DEFAULT_VALUE);
                String defaultValue = "";
                String format = "";
                if (defaultValueNode != null) {
                    defaultValue = defaultValueNode.asText();
                }
                JsonNode formatNode = propertyNode.get(FORMAT);
                if (formatNode != null) {
                    format = formatNode.asText();
                }

                FieldDto f = new FieldDto.Builder().setName(propertyName)
                    .setType(propertyType)
                    .setDefaultValue(defaultValue)
                    .setFormat(format)
                    .build();
                fields.add(f);
            }
            datatype.setFields(fields);
            result.add(datatype);
        }
        return result;
    }

    private String extractType(JsonNode propertyNode) {
        String result = "";
        if (propertyNode.has(TYPE)) {
            JsonNode typeNode = propertyNode.get(TYPE);
            String type = typeNode.asText();
            // arrays
            if (type.equals(ARRAY)) {
                JsonNode itemsNode = propertyNode.get(ITEMS);
                if (itemsNode.has(TYPE)) {
                    type = itemsNode.get(TYPE).asText();
                } else {
                    type = itemsNode.get(REF).asText().substring(REF_SCHEMA_LENGTH);
                }
                result = type + "[]";
            } else {
                // primitives
                result = type;
            }
        } else {
            // custom objects
            result = propertyNode.get(REF).asText().substring(21);
        }
        return StringUtils.capitalize(result);
    }

}
