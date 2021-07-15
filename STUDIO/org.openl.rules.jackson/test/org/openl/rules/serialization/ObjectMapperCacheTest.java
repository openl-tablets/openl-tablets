package org.openl.rules.serialization;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


public class ObjectMapperCacheTest {

    static class BindingClasses {
        private String key;
        private String value;
    }

    static class KeyClass {
        private String field;

        public KeyClass(String field) {
            this.field = field;
        }
    }

    @Test
    public void getObjectMapperTest_notNull() {
        KeyClass key = new KeyClass("Project1");
        ObjectMapper objectMapper1 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper1);
    }

    @Test
    public void getObjectMapperTest_Cached() {
        KeyClass key = new KeyClass("Project2");
        ObjectMapper objectMapper1 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper1);
        ObjectMapper objectMapper2 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper2);
        Assert.assertEquals(objectMapper1, objectMapper2);
    }

    @Test
    public void getObjectMapperTest_GC_keep() {
        KeyClass key = new KeyClass("Project3");
        ObjectMapper objectMapper1 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper2 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertEquals(objectMapper1, objectMapper2);
    }

    @Test
    public void getObjectMapperTest_GC_no_longer() {
        KeyClass key = new KeyClass("Project3");
        ObjectMapper objectMapper1 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Runtime rt = Runtime.getRuntime();
        key = null;
        rt.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        key = new KeyClass("Project3");
        ObjectMapper objectMapper2 = ObjectMapperCache.getObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotEquals(objectMapper1, objectMapper2);
    }

}
