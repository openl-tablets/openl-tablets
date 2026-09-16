package org.openl.rules.table.formatters;

import java.lang.reflect.Array;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ArrayFormatter implements IFormatter {


    @Getter
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
                log.debug("Should be an array: {}", value);
                return null;
            }

            Object[] array = ArrayTool.toArray(value);

            String[] elementResults = new String[array.length];

            for (var i = 0; i < array.length; i++) {
                var element = array[i];
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
    /**
     * The values the whole text stands for, or {@code null} where one of them stands for none.
     *
     * <p>An element left empty is a value of its own — the array holds nothing at that place — but an
     * element carrying text that stands for no value makes the whole text no array: answering with a blank
     * there would say less than the author wrote, and leave the reader nothing to correct.
     */
    public Object parse(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        var elements = ArraySplitter.split(value);
        var parsed = (Object[]) Array.newInstance(elementType, elements.length);
        for (var at = 0; at < elements.length; at++) {
            parsed[at] = elementFormat.parse(elements[at]);
            if (parsed[at] == null && StringUtils.isNotBlank(elements[at])) {
                return null;
            }
        }
        return parsed;
    }
}
