package org.openl.rules.jackson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.serialization.JsonUtils;

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
        Assert.assertEquals(
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
        Assert.assertEquals("25", JsonUtils.toJSON(new ByteValue((byte) 25)));
        Assert.assertEquals("25", JsonUtils.toJSON(new ShortValue((short) 25)));
        Assert.assertEquals("25", JsonUtils.toJSON(new IntValue(25)));
        Assert.assertEquals("25", JsonUtils.toJSON(new LongValue(25)));
        Assert.assertEquals("2.5", JsonUtils.toJSON(new FloatValue(2.5f)));
        Assert.assertEquals("2.5", JsonUtils.toJSON(new DoubleValue(2.5d)));

        Assert.assertEquals(new ByteValue((byte) 25), JsonUtils.fromJSON("25", ByteValue.class));
        Assert.assertEquals(new ShortValue((short) 25), JsonUtils.fromJSON("25", ShortValue.class));
        Assert.assertEquals(new IntValue(25), JsonUtils.fromJSON("25", IntValue.class));
        Assert.assertEquals(new LongValue(25), JsonUtils.fromJSON("25", LongValue.class));
        Assert.assertEquals(new FloatValue(2.5f), JsonUtils.fromJSON("2.5", FloatValue.class));
        Assert.assertEquals(new DoubleValue(2.5d), JsonUtils.fromJSON("2.5", DoubleValue.class));

    }

}
