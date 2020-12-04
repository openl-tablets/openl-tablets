package org.openl.spring.env;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

public class RefPropertySourceTest {
    @Test
    public void noSources() {
        RefPropertySource ref = new RefPropertySource(new MutablePropertySources());
        Assert.assertNull(ref.getProperty(""));
        Assert.assertNull(ref.getProperty("."));
        Assert.assertNull(ref.getProperty(".b"));
        Assert.assertNull(ref.getProperty("b"));
        Assert.assertNull(ref.getProperty("b."));
        Assert.assertNull(ref.getProperty("b.b"));
        Assert.assertNull(ref.getProperty(".$ref"));
        Assert.assertNull(ref.getProperty("b.$ref"));
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
        RefPropertySource ref = new RefPropertySource(propertySources);
        propertySources.addLast(ref);
        Assert.assertNull(ref.getProperty(""));
        Assert.assertNull(ref.getProperty("."));
        Assert.assertNull(ref.getProperty(".b"));
        Assert.assertNull(ref.getProperty("b"));
        Assert.assertNull(ref.getProperty("b."));
        Assert.assertNull(ref.getProperty("b.b"));
        Assert.assertNull(ref.getProperty(".$ref"));
        Assert.assertNull(ref.getProperty("b.$ref"));
        Assert.assertNull(ref.getProperty("abc"));
        Assert.assertNull(ref.getProperty("abc.def"));
        Assert.assertNull(ref.getProperty(".ghi"));
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
        RefPropertySource ref = new RefPropertySource(propertySources);
        propertySources.addLast(ref);
        Assert.assertEquals("A", ref.getProperty("xyz"));
        Assert.assertEquals("B", ref.getProperty("xyz.def"));
        Assert.assertEquals("C", ref.getProperty("xyz.gh.i"));
        Assert.assertNull(ref.getProperty("xyz.ghz"));
        Assert.assertNull(ref.getProperty("xyz.yvw"));
        Assert.assertEquals("1", ref.getProperty("xyz.yvw.x"));
        Assert.assertEquals("2", ref.getProperty("xyz.yvw.y"));
        Assert.assertNull(ref.getProperty("xyz.yvw.z"));

        Assert.assertEquals("1", ref.getProperty("klq.x"));
        Assert.assertEquals("2", ref.getProperty("klq.y"));
        Assert.assertNull(ref.getProperty("klq.zz"));

        Assert.assertNull(ref.getProperty(".$ref"));
        Assert.assertNull(ref.getProperty("b.$ref"));
        Assert.assertNull(ref.getProperty("abc"));
        Assert.assertNull(ref.getProperty("abc.def"));
    }
}
