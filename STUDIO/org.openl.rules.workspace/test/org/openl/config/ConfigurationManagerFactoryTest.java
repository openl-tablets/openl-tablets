package org.openl.config;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Map;

public class ConfigurationManagerFactoryTest {

    private static final String CONFIG = "META-INF/maven/org.slf4j/slf4j-api/pom.properties";

    @Test
    public void inClasspathWithoutSystemsProps() {
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(false, null, "");
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(CONFIG);
        Map<String, Object> properties = configurationManager.getProperties();
        assertEquals(3, properties.size());
        assertEquals("org.slf4j", properties.get("groupId"));
        assertEquals("slf4j-api", properties.get("artifactId"));
    }

    @Test
    public void inClasspathWithSystemsProps() {
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(true, null, "");
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(CONFIG);
        Map<String, Object> properties = configurationManager.getProperties();
        assertTrue(properties.size() > 3);
        assertEquals("org.slf4j", properties.get("groupId"));
        assertEquals("slf4j-api", properties.get("artifactId"));
    }
}
