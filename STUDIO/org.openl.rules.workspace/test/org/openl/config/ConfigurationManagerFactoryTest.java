package org.openl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class ConfigurationManagerFactoryTest {

    private static final String CONFIG = "META-INF/maven/org.slf4j/slf4j-api/pom.properties";

    @Test
    public void inClasspathWithSystemsProps() {
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(null, "");
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(CONFIG);
        Map<String, Object> properties = configurationManager.getProperties();
        assertTrue(properties.size() > 3);
        assertEquals("org.slf4j", properties.get("groupId"));
        assertEquals("slf4j-api", properties.get("artifactId"));
    }
}
