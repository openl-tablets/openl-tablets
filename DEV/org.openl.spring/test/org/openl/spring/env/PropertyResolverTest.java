package org.openl.spring.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {"openl.config.location=classpath:firewall.properties"})
@ContextConfiguration(initializers = PropertySourcesLoader.class)
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
    }
}
