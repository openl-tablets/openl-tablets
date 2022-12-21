package org.openl.rules.table.formatters;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.helpers.ArraySplitter;
import org.openl.util.ArrayTool;
import org.openl.util.StringUtils;
import org.openl.util.formatters.IFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A formatter for converting an array of elements, represented by <code>{@link String}</code> to an array of specified
 * type (method <code>{@link #parse(String)}</code>). <br>
 * Also it provides the back convertion from specified type to a <code>{@link String}</code>, in outcome result elements
 * will be separated by comma (method <code>{@link #format(Object)}</code>).
 */
public class ArrayFormatter implements IFormatter {

    private static final Logger LOG = LoggerFactory.getLogger(ArrayFormatter.class);

    private final IFormatter elementFormat;

    /**
     * @param elementFormat formatter for the component type of array.
     */
    public ArrayFormatter(IFormatter elementFormat) {
        this.elementFormat = elementFormat;
    }

    /**
     * Converts an input array of elements to <code>{@link String}</code>. Elements in the return value will be separated
     * by comma. Null safety.
     *
     * @param value array of elements that should be represented as <code>{@link String}</code>.
     * @return <code>{@link String}</code> representation of the income array. <code>NULL</code> if the income value is
     *         <code>NULL</code> or if income value is not an array.
     */
    @Override
    public String format(Object value) {
        String result = null;
        if (value != null) {
            if (!value.getClass().isArray()) {
                LOG.debug("Should be an array: {}", value);
                return null;
            }

            Object[] array = ArrayTool.toArray(value);

            String[] elementResults = new String[array.length];

            for (int i = 0; i < array.length; i++) {
                Object element = array[i];
                elementResults[i] = elementFormat.format(element);
                result = String.join(",", elementResults);
            }
        }
        return result;
    }

    /**
     * @param value <code>{@link String}</code> representation of the array.
     * @return array of elements. <code>NULL</code> if input is empty or can`t get the component type of the array.
     */
    @Override
    public Object parse(String value) {
        Object result = null;
        if (StringUtils.isNotBlank(value)) {
            String[] elementValues = ArraySplitter.split(value);

            List<Object> elements = new ArrayList<>();
            Class<?> elementType = null;

            for (String elementValue : elementValues) {
                Object element = elementFormat.parse(elementValue);
                elements.add(element);
                Class<?> type = element.getClass();
                if (elementType == null) {
                    elementType = type;
                } else if (elementType != type) {
                    elementType = Object.class;
                }
            }

            if (elementType == null) {
                return null;
            }

            Object[] resultArray = (Object[]) Array.newInstance(elementType, elements.size());
            result = elements.toArray(resultArray);
        }

        return result;
    }

    public IFormatter getElementFormat() {
        return elementFormat;
    }
}
