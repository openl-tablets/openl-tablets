package org.openl.rules.serialization.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.serialization.JsonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtilsTest {

    @Test
    public void defaultJacksonObjectMapperTest() throws NoSuchMethodException,
                                                 InvocationTargetException,
                                                 IllegalAccessException {
        Method getDefaultJacksonObjectMapperMethod = JsonUtils.class.getDeclaredMethod("getDefaultJacksonObjectMapper");
        getDefaultJacksonObjectMapperMethod.setAccessible(true);
        Assert.assertNotEquals(
            JsonUtils.class.getTypeName() + "." + getDefaultJacksonObjectMapperMethod
                .getName() + " must return different instances.",
            getDefaultJacksonObjectMapperMethod.invoke(null),
            getDefaultJacksonObjectMapperMethod.invoke(null));
    }

    @Test
    public void spreadsheetResultTest() throws Exception {

        String[] columnNames = new String[] { "Column1", "Column2" };
        String[] rowNames = new String[] { "Row1", "Row2" };
        Object[][] results = new Object[][] { new Object[] { "ROW1COLUMN1", "ROW1COLUMN2" },
                new Object[] { "ROW2COLUMN1", "ROW2COLUMN2" } };

        SpreadsheetResult spreadsheetResult = new SpreadsheetResult(results, rowNames, columnNames);

        String json = JsonUtils.toJSON(spreadsheetResult);
        assertEquals(
            "{\"results\":[[\"ROW1COLUMN1\",\"ROW1COLUMN2\"],[\"ROW2COLUMN1\",\"ROW2COLUMN2\"]],\"columnNames\":[\"Column1\",\"Column2\"],\"rowNames\":[\"Row1\",\"Row2\"]}",
            json);

        SpreadsheetResult spResult = JsonUtils.fromJSON(
            "{\"results\":[[\"ROW1COLUMN1\",\"ROW1COLUMN2\"],[\"ROW2COLUMN1\",\"ROW2COLUMN2\"]],\"columnNames\":[\"Column1\",\"Column2\"],\"rowNames\":[\"Row1\",\"Row2\"],\"columnTitles\":[\"Row1\",\"Row2\"]}",
            SpreadsheetResult.class);

        Assert.assertArrayEquals(spreadsheetResult.getColumnNames(), spResult.getColumnNames());
        Assert.assertArrayEquals(spreadsheetResult.getRowNames(), spResult.getRowNames());
        Assert.assertArrayEquals(spreadsheetResult.getResults(), spResult.getResults());

        spResult = JsonUtils.fromJSON(json, SpreadsheetResult.class);

        Assert.assertArrayEquals(spreadsheetResult.getColumnNames(), spResult.getColumnNames());
        Assert.assertArrayEquals(spreadsheetResult.getRowNames(), spResult.getRowNames());
        Assert.assertArrayEquals(spreadsheetResult.getResults(), spResult.getResults());

    }

    @Test
    public void toJSONTest() throws JsonProcessingException {
        assertEquals("{\"model\":\"BMW\",\"year\":null}", JsonUtils.toJSON(new Car("BMW", null)));
        assertEquals("{\"model\":\"BMW\",\"year\":null}",
            JsonUtils.toJSON(new Car("BMW", null), new Class[] { Car.class }));
        assertEquals("{\"@class\":\"org.openl.rules.serialization.jackson.JsonUtilsTest$Car\",\"model\":\"BMW\",\"year\":null}",
            JsonUtils.toJSON(new Car("BMW", null), new Class[] { Car.class, Track.class }, true));
    }

    @Test
    public void fromJSONTest() throws IOException {
        final Car expected = new Car("BMW", null);
        assertEquals(expected, JsonUtils.fromJSON("{\"model\":\"BMW\",\"year\":null}", Car.class));
        assertEquals(expected,
            JsonUtils.fromJSON("{\"model\":\"BMW\",\"year\":null}", Car.class, new Class[] { Car.class }));
        assertEquals(expected,
            JsonUtils.fromJSON(
                "{\"@class\":\"org.openl.rules.jackson.JsonUtilsTest$Car\",\"model\":\"BMW\",\"year\":null}",
                Car.class,
                new Class[] { Car.class }));
    }

    @Test
    public void splitJSONTest() throws IOException {
        Map<String, String> actual = JsonUtils.splitJSON("{\"context\":{}, \"car\":{\"model\":\"BMW\",\"year\":null}}");
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertEquals("{}", actual.get("context"));
        assertEquals("{\"model\":\"BMW\",\"year\":null}", actual.get("car"));
    }

    public static class Track extends Car {

    }

    public static class Car {

        private String model;
        private String year;

        public Car(String model, String year) {
            this.model = model;
            this.year = year;
        }

        public Car() {
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Car car = (Car) o;
            return Objects.equals(model, car.model) && Objects.equals(year, car.year);
        }

        @Override
        public int hashCode() {
            return Objects.hash(model, year);
        }
    }

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
        ObjectMapper objectMapper1 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper1);
    }

    @Test
    public void getObjectMapperTest_Cached() {
        KeyClass key = new KeyClass("Project2");
        ObjectMapper objectMapper1 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper1);
        ObjectMapper objectMapper2 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotNull(objectMapper2);
        Assert.assertEquals(objectMapper1, objectMapper2);
    }

    @Test
    public void getObjectMapperTest_GC_keep() {
        KeyClass key = new KeyClass("Project3");
        ObjectMapper objectMapper1 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper2 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertEquals(objectMapper1, objectMapper2);
    }

    @Test
    public void getObjectMapperTest_GC_no_longer() {
        KeyClass key = new KeyClass("Project4");
        ObjectMapper objectMapper1 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Runtime rt = Runtime.getRuntime();
        key = null;
        rt.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        key = new KeyClass("Project4");
        ObjectMapper objectMapper2 = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Assert.assertNotEquals(objectMapper1, objectMapper2);
    }

    @Test
    public void splitJSONTest_CachedObjectMapper() throws IOException {
        KeyClass key = new KeyClass("Project4");
        ObjectMapper objectMapper = JsonUtils.getCachedObjectMapper(key, new Class[]{BindingClasses.class});
        Map<String, String> actual = JsonUtils.splitJSON("{\"context\":{}, \"car\":{\"model\":\"BMW\",\"year\":null}}", objectMapper);
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertEquals("{}", actual.get("context"));
        assertEquals("{\"model\":\"BMW\",\"year\":null}", actual.get("car"));
    }

}
