package org.openl.spring.env;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

public class SysEnvRefPropertySourceTest {

    @Test
    public void test() {
        HashMap<String, Object> environment = new HashMap<String, Object>() {
            {
                put("ABC", "1");
                put("ABC_$REF", "2");
                put("WWW__REF_", "3");
                put("WWW__FOO__GAZ", "4");
            }
        };

        SysEnvRefPropertySource refEnvSource = new SysEnvRefPropertySource(environment);

        assertEquals("1", refEnvSource.getProperty("abc"));
        assertEquals("1", refEnvSource.getProperty("ABC"));
        assertEquals("2", refEnvSource.getProperty("ABC_$REF"));
        assertEquals("2", refEnvSource.getProperty("abc_$ref"));
        assertEquals("3", refEnvSource.getProperty("WWW__REF_"));
        assertEquals("4", refEnvSource.getProperty("www__foo__gaz"));

        assertEquals("3", refEnvSource.getProperty("WWW_$REF"));
        assertEquals("3", refEnvSource.getProperty("www_$ref"));
        assertEquals("3", refEnvSource.getProperty("WWW.$REF"));
        assertEquals("3", refEnvSource.getProperty("www.$ref"));

        assertEquals("4", refEnvSource.getProperty("WWW_$FOO_GAZ"));
        assertEquals("4", refEnvSource.getProperty("www_$foo_gaz"));
        assertEquals("4", refEnvSource.getProperty("WWW.$FOO.GAZ"));
        assertEquals("4", refEnvSource.getProperty("www.$foo.gaz"));
    }

}
