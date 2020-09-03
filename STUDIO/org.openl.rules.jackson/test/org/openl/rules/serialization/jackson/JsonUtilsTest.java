package org.openl.rules.serialization.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.openl.meta.*;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.serialization.JsonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonUtilsTest {

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
    public void openLValueTypesTest() throws Exception {
        assertEquals("25", JsonUtils.toJSON(new ByteValue((byte) 25)));
        assertEquals("25", JsonUtils.toJSON(new ShortValue((short) 25)));
        assertEquals("25", JsonUtils.toJSON(new IntValue(25)));
        assertEquals("25", JsonUtils.toJSON(new LongValue(25)));
        assertEquals("2.5", JsonUtils.toJSON(new FloatValue(2.5f)));
        assertEquals("2.5", JsonUtils.toJSON(new DoubleValue(2.5d)));

        assertEquals(new ByteValue((byte) 25), JsonUtils.fromJSON("25", ByteValue.class));
        assertEquals(new ShortValue((short) 25), JsonUtils.fromJSON("25", ShortValue.class));
        assertEquals(new IntValue(25), JsonUtils.fromJSON("25", IntValue.class));
        assertEquals(new LongValue(25), JsonUtils.fromJSON("25", LongValue.class));
        assertEquals(new FloatValue(2.5f), JsonUtils.fromJSON("2.5", FloatValue.class));
        assertEquals(new DoubleValue(2.5d), JsonUtils.fromJSON("2.5", DoubleValue.class));
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

}
