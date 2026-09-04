package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

class RefPropertySourceTest {
    @Test
    void noSources() {
        var ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), new MutablePropertySources());
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
    void noRefs() {
        var propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", Map.of(
                "abc", "1",
                "abc.def", "2",
                ".ghi", "3")));
        var ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
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
    void refs() {
        var propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", Map.of(
                "abc", "A",
                "abc.def", "B",
                "abc.gh.i", "C",
                "abc.yvw.y", "Y",
                "abc.yvw.z", "Z",
                "klq.$ref", "mno.www")));
        propertySources.addLast(new MapPropertySource("B", Map.of(
                "xyz.$ref", "abc",
                "xyz.yvw.$ref", "mno.www",
                "mno.www.x", "1",
                "mno.www.y", "2")));
        var ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
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
    void multiLevelRefs() {
        var propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource("A", Map.ofEntries(
                // root
                Map.entry("abc", "1"),
                Map.entry("abc.def", "2"),
                Map.entry("foo.bar", "11"),
                // level 1
                Map.entry("q.$ref", "abc"),
                Map.entry("q.bar", "111"),
                Map.entry("q.foo2.$ref", "foo"),
                // level 2
                Map.entry("www.$ref", "q"),
                Map.entry("www.len", "21"),
                Map.entry("www.fff.$ref", "qqq"),
                // level 3
                Map.entry("qqq.$ref", "www"),
                Map.entry("qqq.gg.$ref", "abc"),
                Map.entry("qqq.dd", "pam"))));

        var ref = new RefPropertySource(new PropertySourcesPropertyResolver(null), propertySources);
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
