package org.openl.rules.table.xls.formatters;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.ArrayTool;
import org.openl.util.StringTool;

/**
 * 
 * Xls array format is a formatter for converting an array of elements,
 * represented by <code>{@link String}</code> (elements are separated by
 * <code>{@link #ARRAY_ELEMENTS_SEPARATOR}</code> escaper for the separator is
 * <code>{@link #ARRAY_ELEMENTS_SEPARATOR_ESCAPER}</code>) to an array of
 * specified type (method <code>{@link #parse(String)}</code>). <br>
 * Also it provides the back convertion from specified type to a
 * <code>{@link String}</code>, in outcome result elements will separated by
 * <code>{@link #ARRAY_ELEMENTS_SEPARATOR}</code> (method
 * <code>{@link #format(Object)}</code>).
 * 
 */
public class XlsArrayFormatter extends AXlsFormatter {

    private static final Log LOG = LogFactory.getLog(XlsArrayFormatter.class);

    /**
     * Constant for escaping {@link #ARRAY_ELEMENTS_SEPARATOR} of elements. It
     * is needed when the element contains separator as part of object name,
     * e.g: Mike\\,Sara`s Son.
     * 
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";

    /**
     * Separator for elements of array, represented as
     * <code>{@link String}</code>.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";

    private AXlsFormatter elementFormat;

    /**
     * @param elementFormat formatter for the component type of array.
     */
    public XlsArrayFormatter(AXlsFormatter elementFormat) {
        this.elementFormat = elementFormat;
    }

    /**
     * Converts an input array of elements to <code>{@link String}</code>.
     * Elements in the return value will separated by
     * {@link #ARRAY_ELEMENTS_SEPARATOR}. Null safety.
     * 
     * @param value array of elements that should be represented as
     *            <code>{@link String}</code>.
     * @return <code>{@link String}</code> representation of the income array.
     *         <code>NULL</code> if the income value is <code>NULL</code> or if
     *         income value is not an array.
     */
    public String format(Object value) {
        String result = null;
        if (value != null) {
            if (!(value.getClass().isArray())) {
                LOG.error(String.format("Should be an array: %s", value.toString()));
                return result;
            }
            
            Object[] array = ArrayTool.toArray(value);

            String[] elementResults = new String[array.length];

            for (int i = 0; i < array.length; i++) {
                Object element = array[i];
                elementResults[i] = elementFormat.format(element);
                result = StringUtils.join(elementResults, ",");
            }
        }
        return result;
    }

    /**
     * 
     * @param value <code>{@link String}</code> representation of the array.
     * @return array of elements. <code>NULL</code> if input is empty or can`t
     *         get the component type of the array.
     */
    public Object parse(String value) {
        Object result = null;
        if (StringUtils.isNotBlank(value)) {
            String[] elementValues = StringTool.splitAndEscape(value,
                ARRAY_ELEMENTS_SEPARATOR,
                ARRAY_ELEMENTS_SEPARATOR_ESCAPER);

            List<Object> elements = new ArrayList<Object>();
            Class<?> elementType = null;

            for (String elementValue : elementValues) {
                Object element = elementFormat.parse(elementValue);
                elements.add(element);
                elementType = element.getClass();
            }

            if (elementType == null) {
                return result;
            }

            Object[] resultArray = (Object[]) Array.newInstance(elementType, elements.size());

            try {
                result = elements.toArray(resultArray);
            } catch (ArrayStoreException e) {
                // Ignore exception. An exception occurs when element type is
                // different for elements of array.
            }
        }

        return result;
    }
}
