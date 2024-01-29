package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestPropertySource(properties = {"openl.config.location=classpath:firewall.properties"})
@SpringJUnitConfig(initializers = PropertySourcesLoader.class)
public class PropertyResolverTest {

    @Autowired
    PropertyResolver propertyResolver;

    @Test
    public void test() {
        assertEquals("value1", propertyResolver.getProperty("key1"));
        assertEquals("value2", propertyResolver.getProperty("key2"));
        assertEquals("myValue", propertyResolver.getProperty("my.prop"));
        assertNull(propertyResolver.getProperty("MY_PROP"));
        assertEquals("my Value 2", propertyResolver.getProperty("MY_PROP2"));
        assertNull(propertyResolver.getProperty("my.prop2"));
        assertEquals("base.prop", propertyResolver.getProperty("my.prop.$ref"));
        assertEquals("Reference", propertyResolver.getProperty("my.prop.by.ref"));
        assertEquals("Reference", propertyResolver.getProperty("base.prop.by.ref"));
        assertEquals("https://openl-tablets.org", propertyResolver.getProperty("openl.site"));
        assertEquals("[\\d\\w.$-]+", propertyResolver.getProperty("openl.config.key-pattern.allowed"));

        assertNull(propertyResolver.getProperty("key+3"));// not allowed by default
        assertNull(propertyResolver.getProperty("key:3"));// not allowed by default
        assertNull(propertyResolver.getProperty("key4"));// denied in firewall.properties

        assertEquals("value1", propertyResolver.getProperty("prop1"));
        assertEquals("my.value1.val", propertyResolver.getProperty("prop2"));
        assertEquals("my.def.val", propertyResolver.getProperty("prop3"));
        assertEquals("v1", propertyResolver.getProperty("index"));
        assertEquals("Indexed", propertyResolver.getProperty("prop4"));
        assertNull(propertyResolver.getProperty("loop.v1.val"));

        assertEquals("NoDriver", propertyResolver.getProperty("database.driver"));
        System.setProperty("driver.type", "mssql");
        assertEquals("mssql.Driver", propertyResolver.getProperty("database.driver"));
        System.setProperty("driver.type", "postgres");
        assertEquals("PostgresDriver", propertyResolver.getProperty("database.driver"));
        System.setProperty("driver.type", "unknown");
        assertNull(propertyResolver.getProperty("database.driver"));
        System.setProperty("driver.type", "");
        assertNull(propertyResolver.getProperty("database.driver"));
        System.clearProperty("driver.type");
        assertEquals("NoDriver", propertyResolver.getProperty("database.driver"));
    }
}
