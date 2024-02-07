package org.openl.rules.table.formatters;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.helpers.ArraySplitter;
import org.openl.util.ArrayTool;
import org.openl.util.StringUtils;
import org.openl.util.formatters.IFormatter;

/**
 * A formatter for converting an array of elements, represented by <code>{@link String}</code> to an array of specified
 * type (method <code>{@link #parse(String)}</code>). <br>
 * Also it provides the back convertion from specified type to a <code>{@link String}</code>, in outcome result elements
 * will be separated by comma (method <code>{@link #format(Object)}</code>).
 */
public class ArrayFormatter implements IFormatter {

    private static final Logger LOG = LoggerFactory.getLogger(ArrayFormatter.class);

    private final IFormatter elementFormat;

    private final Class<?> elementType;

    /**
     * @param elementFormat formatter for the component type of array.
     */
    public ArrayFormatter(IFormatter elementFormat, Class<?> elementType) {
        this.elementFormat = elementFormat;
        this.elementType = elementType;
    }

    /**
     * Converts an input array of elements to <code>{@link String}</code>. Elements in the return value will be
     * separated by comma. Null safety.
     *
     * @param value array of elements that should be represented as <code>{@link String}</code>.
     * @return <code>{@link String}</code> representation of the income array. <code>NULL</code> if the income value is
     * <code>NULL</code> or if income value is not an array.
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
        if (StringUtils.isNotBlank(value)) {
            String[] values = ArraySplitter.split(value);
            return Arrays.stream(values)
                    .map(elementFormat::parse)
                    .toArray(e -> (Object[]) Array.newInstance(elementType, e));
        }
        return null;
    }

    public IFormatter getElementFormat() {
        return elementFormat;
    }
}
