package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

public class RefPropertySourceTest {
    @Test
    public void noSources() {
        RefPropertySource ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), new MutablePropertySources());
        assertNull(ref.getProperty(""));
        assertNull(ref.getProperty("."));
        assertNull(ref.getProperty(".b"));
        assertNull(ref.getProperty("b"));
        assertNull(ref.getProperty("b."));
        assertNull(ref.getProperty("b.b"));
        assertNull(ref.getProperty(".$ref"));
        assertNull(ref.getProperty("b.$ref"));
    }

    @Test
    public void noRefs() {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", new HashMap<String, Object>() {
            {
                put("abc", "1");
                put("abc.def", "2");
                put(".ghi", "3");
            }
        }));
        RefPropertySource ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
        propertySources.addLast(ref);
        assertNull(ref.getProperty(""));
        assertNull(ref.getProperty("."));
        assertNull(ref.getProperty(".b"));
        assertNull(ref.getProperty("b"));
        assertNull(ref.getProperty("b."));
        assertNull(ref.getProperty("b.b"));
        assertNull(ref.getProperty(".$ref"));
        assertNull(ref.getProperty("b.$ref"));
        assertNull(ref.getProperty("abc"));
        assertNull(ref.getProperty("abc.def"));
        assertNull(ref.getProperty(".ghi"));
    }

    @Test
    public void refs() {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", new HashMap<String, Object>() {
            {
                put("abc", "A");
                put("abc.def", "B");
                put("abc.gh.i", "C");
                put("abc.yvw.y", "Y");
                put("abc.yvw.z", "Z");
                put("klq.$ref", "mno.www");
            }
        }));
        propertySources.addLast(new MapPropertySource("B", new HashMap<String, Object>() {
            {
                put("xyz.$ref", "abc");
                put("xyz.yvw.$ref", "mno.www");
                put("mno.www.x", "1");
                put("mno.www.y", "2");
            }
        }));
        RefPropertySource ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
        propertySources.addLast(ref);
        assertEquals("A", ref.getProperty("xyz"));
        assertEquals("B", ref.getProperty("xyz.def"));
        assertEquals("C", ref.getProperty("xyz.gh.i"));
        assertNull(ref.getProperty("xyz.ghz"));
        assertNull(ref.getProperty("xyz.yvw"));
        assertEquals("1", ref.getProperty("xyz.yvw.x"));
        assertEquals("2", ref.getProperty("xyz.yvw.y"));
        assertNull(ref.getProperty("xyz.yvw.z"));

        assertEquals("1", ref.getProperty("klq.x"));
        assertEquals("2", ref.getProperty("klq.y"));
        assertNull(ref.getProperty("klq.zz"));

        assertNull(ref.getProperty(".$ref"));
        assertNull(ref.getProperty("b.$ref"));
        assertNull(ref.getProperty("abc"));
        assertNull(ref.getProperty("abc.def"));
    }

    @Test
    public void multiLevelRefs() {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", new HashMap<>() {
            {
                // root
                put("abc", "1");
                put("abc.def", "2");
                put("foo.bar", "11");
                // level 1
                put("q.$ref", "abc");
                put("q.bar", "111");
                put("q.foo2.$ref", "foo");
                // level 2
                put("www.$ref", "q");
                put("www.len", "21");
                put("www.fff.$ref", "qqq");
                // level 3
                put("qqq.$ref", "www");
                put("qqq.gg.$ref", "abc");
                put("qqq.dd", "pam");
            }
        }));

        RefPropertySource ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
        propertySources.addLast(ref);

        assertEquals("1", ref.getProperty("q"));
        assertEquals("2", ref.getProperty("q.def"));

        assertEquals("11", ref.getProperty("q.foo2.bar"));

        assertEquals("1", ref.getProperty("www"));
        assertEquals("2", ref.getProperty("www.def"));
        assertEquals("111", ref.getProperty("www.bar"));
        assertEquals("11", ref.getProperty("www.foo2.bar"));

        assertEquals("21", ref.getProperty("qqq.len"));
        assertEquals("111", ref.getProperty("qqq.bar"));
        // must be null because it's higher than RefPropertySource.MAX_REF_DEPTH
        assertNull(ref.getProperty("qqq"));
        assertNull(ref.getProperty("qqq.def"));
        assertNull(ref.getProperty("qqq.foo2.bar"));

        assertEquals("pam", ref.getProperty("www.fff.dd"));
        assertEquals("1", ref.getProperty("www.fff.gg"));
        assertEquals("2", ref.getProperty("www.fff.gg.def"));
        // must be null because of looping
        assertNull(ref.getProperty("www.fff.fff.len"));
    }
}
