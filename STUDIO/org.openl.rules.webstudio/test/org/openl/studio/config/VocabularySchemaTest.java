package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.OpenLClassLoader;
import org.openl.gen.FieldDescription;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * The JSON schema of a generated datatype bean carries the values of a vocabulary field the way the bean generator
 * writes them: on the property for a scalar, on the items for an array.
 */
class VocabularySchemaTest {

    private static final List<String> VALUES = List.of("bla1", "bla2", "bla3");

    @Test
    void publishesTheValuesOfAVocabularyFieldAndOfItsArray() throws Exception {
        var objectMapper = new ObjectMapper();
        var generator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);

        var schema = generator.generateSchema(vocabularyBean());

        var properties = schema.get("properties");
        assertEquals(VALUES, values(properties.get("kind").get("enum")));
        assertNull(properties.get("kinds").get("enum"), "the array itself takes no word");
        assertEquals(VALUES, values(properties.get("kinds").get("items").get("enum")));
    }

    /** A spreadsheet result bean, which carries the annotations on its getters, describes a vocabulary cell too. */
    @Test
    void publishesTheValuesOfAVocabularyCell() throws Exception {
        var objectMapper = new ObjectMapper();
        var generator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);

        var schema = generator.generateSchema(spreadsheetResultBean());

        // A plain mapper names the properties after the getters.
        var properties = schema.get("properties");
        assertEquals(VALUES, values(properties.get("kind").get("enum")));
        assertEquals(VALUES, values(properties.get("kinds").get("items").get("enum")));
        assertNull(properties.get("note").get("enum"));
    }

    /** A datatype bean with a field of a vocabulary type and a field of an array of it, as OpenL generates it. */
    private static Class<?> vocabularyBean() throws Exception {
        var byteCode = new JavaBeanClassBuilder("org.openl.generated.beans.VocabularyBean")
                .addFields(Map.of("kind", vocabularyField(String.class), "kinds", vocabularyField(String[].class)))
                .byteCode();
        var classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        return ClassLoaderUtils.defineClass("org.openl.generated.beans.VocabularyBean", byteCode, classLoader);
    }

    /** The bean of {@code calc}, a spreadsheet with a cell of a vocabulary type, a cell of an array of it and a plain one. */
    private static Class<?> spreadsheetResultBean() {
        var module = new RulesEngineFactory<>("test/rules/EPBDS-16692/Vocabulary.xlsx").getCompiledOpenClass().getOpenClass();
        var calc = module.getMethods().stream().filter(method -> method.getName().equals("calc")).findFirst().orElseThrow();
        return ((CustomSpreadsheetResultOpenClass) calc.getType()).getBeanClass();
    }

    private static FieldDescription vocabularyField(Class<?> type) {
        var values = VALUES.toArray(new String[0]);
        return new FieldDescription(type.getName(), null, null, null, null, null, values, null, false, false);
    }

    private static List<?> values(JsonNode enumeration) {
        return new ObjectMapper().convertValue(enumeration, List.class);
    }
}
