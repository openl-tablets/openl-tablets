package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenInterface;
import org.openl.util.ClassUtils;
import org.openl.vm.SimpleRuntimeEnv;

class TablePropertiesJsonTest {

    private final SimpleRuntimeEnv env = new SimpleRuntimeEnv();

    @Test
    void writesThePropertiesOfARuleAsItsGetters() throws Exception {
        var json = write(ruleProperties());

        assertEquals(getters(), names(json));
        assertEquals("CA", json.get("state").get(0).asText());
        assertEquals("Premium", json.get("nature").asText());
    }

    @Test
    void writesThePropertiesOfARuleADatatypeHolds() throws Exception {
        var json = write(holder(ruleProperties()));

        assertEquals(getters(), names(json.get("properties")));
        assertEquals("Premium", json.get("properties").get("nature").asText());
        assertEquals("Premium", json.get("versions").get(0).get("nature").asText());
    }

    @Test
    void writesThePropertiesATestBuildsInADatatype() throws Exception {
        var json = write(holder(testProperties()));

        assertEquals("Premium", json.get("properties").get("nature").asText());
        assertEquals("Premium", json.get("versions").get(0).get("nature").asText());
        assertFalse(json.get("properties").has("@class"));
        assertFalse(json.get("versions").get(0).has("@class"));
    }

    private static JsonNode write(Object value) throws Exception {
        var mapper = new JacksonObjectMapperFactoryBean().createJacksonObjectMapper();
        return mapper.readTree(mapper.writeValueAsString(value));
    }

    /** The names of the properties: those of the getters of TableProperties, as the JSON always had them. */
    private static Set<String> getters() {
        return Arrays.stream(TableProperties.class.getMethods())
                .map(method -> ClassUtils.toFieldName(method.getName()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> names(JsonNode json) {
        var names = new TreeSet<String>();
        json.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /** The properties as $properties returns them. */
    private static TableProperties ruleProperties() {
        var properties = new org.openl.rules.table.properties.TableProperties();
        properties.setState(UsStatesEnum.CA);
        properties.setNature("Premium");
        return new TablePropertiesImpl(properties);
    }

    /** The properties as a Test table builds them. */
    private TableProperties testProperties() {
        var type = (JavaOpenInterface) JavaOpenClass.getOpenClass(TableProperties.class);
        var properties = (TableProperties) type.newInstance(env);
        type.getFieldToFill(type.getField("nature")).set(properties, "Premium", env);
        return properties;
    }

    /** A bean as OpenL generates it for a Datatype with the fields of the TableProperties type. */
    private static Object holder(TableProperties properties) throws Exception {
        var name = "org.openl.generated.beans.PropertiesHolder";
        var byteCode = new JavaBeanClassBuilder(name)
                .addField("properties", new FieldDescription(TableProperties.class.getName()))
                .addField("versions", new FieldDescription(TableProperties[].class.getName()))
                .byteCode();
        var holderClass = ClassLoaderUtils.defineClass(name, byteCode, TablePropertiesJsonTest.class.getClassLoader());
        var holder = holderClass.getDeclaredConstructor().newInstance();
        holderClass.getMethod("setProperties", TableProperties.class).invoke(holder, properties);
        holderClass.getMethod("setVersions", TableProperties[].class)
                .invoke(holder, (Object) new TableProperties[]{properties});
        return holder;
    }
}
