package org.openl.rules.table.formatters;

import org.junit.Test;
import org.openl.util.formatters.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormattersManagerTest {

    private enum TestValues {
        FIRST_VALUE,
        SECOND_VALUE
    }

    @Test
    public void testDouble() {
        Double dd = 12.345;
        IFormatter formatter = FormattersManager.getFormatter(dd);
        assertTrue(formatter instanceof SmartNumberFormatter);
    }

    @Test
    public void testNull() {
        IFormatter formatter = FormattersManager.getFormatter((Object) null);
        assertTrue(formatter instanceof FormatterAdapter);
    }

    @Test
    public void testNaN() {
        assertEquals("NaN", FormattersManager.getFormatter(Double.NaN).format(Double.NaN));
        assertEquals("NaN", FormattersManager.getFormatter(Float.NaN).format(Float.NaN));
    }

    @Test
    public void testString() {
        IFormatter formatter = FormattersManager.getFormatter("text");
        assertTrue(formatter instanceof FormatterAdapter);
    }

    @Test
    public void testDate() {
        Calendar date = Calendar.getInstance();
        IFormatter formatter = FormattersManager.getFormatter(date.getTime());
        assertTrue(formatter instanceof DateFormatter);
    }

    @Test
    public void testBoolean() {
        IFormatter formatter = FormattersManager.getFormatter(Boolean.TRUE);
        assertTrue(formatter instanceof BooleanFormatter);
    }

    @Test
    public void testArray() {
        Integer[] intArray = new Integer[]{12, 34};
        IFormatter formatter = FormattersManager.getFormatter(intArray);
        assertTrue(formatter instanceof ArrayFormatter);
    }

    @Test
    public void testEnums() {
        IFormatter formatter = FormattersManager.getFormatter(TestValues.FIRST_VALUE);
        assertTrue(formatter instanceof EnumFormatter);
    }

    @Test
    public void testFormat() {
        assertEquals("null", FormattersManager.format(null));
        assertEquals("Str", FormattersManager.format("Str"));
        assertEquals("1", FormattersManager.format(1));
        assertEquals("0.1", FormattersManager.format(0.1));
        assertEquals("2", FormattersManager.format(BigDecimal.valueOf(2)));
        assertEquals("true", FormattersManager.format(true));
        assertEquals("1.2105263157894737", FormattersManager.format(23d / 19d));
        assertEquals("07/12/1980", FormattersManager.format(new GregorianCalendar(1980,6,12).getTime()));
        assertEquals("foo,bar", FormattersManager.format(new String[]{"foo", "bar"}));
        assertEquals("Object(id=0)[]", FormattersManager.format(new Object()));
        assertEquals("Arrays.ArrayList<String>{BAR, FOO}", FormattersManager.format(Arrays.asList("BAR", "FOO")));
    }
}
