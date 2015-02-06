package org.openl.rules.convertor;

import org.apache.commons.lang3.StringUtils;
import org.openl.util.StringTool;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A converter for arrays. It converts strings to an array of a specified type and vice versa.
 * E.g. for int[]: "1,2,4,8" <==> int[]{1,2,4,8}
 *
 * @author Yury Molchan
 */
class String2ArrayConvertor<T> implements IString2DataConvertor<T[]> {

    /**
     * Constant for escaping {@link #ARRAY_ELEMENTS_SEPARATOR} of elements. It is needed when the element contains
     * separator as part of object name, e.g: Mike\\,Sara`s Son.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";

    /**
     * Separator for elements of array, represented as <code>{@link String}</code>.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";

    private Class<T> componentType;

    /**
     * @param componentType a component type of an array.
     */
    public String2ArrayConvertor(Class<T> componentType) {
        this.componentType = componentType;
    }

    /**
     * Converts an input array of elements to <code>{@link String}</code>. Elements in the return value will separated by
     * {@link #ARRAY_ELEMENTS_SEPARATOR}. Null safety.
     *
     * @param data array of elements that should be represented as <code>{@link String}</code>.
     * @return <code>{@link String}</code> representation of the income array. <code>NULL</code> if the income value is
     * <code>NULL</code> or if income value is not an array.
     */
    @Override
    public String format(T[] data, String format) {
        if (data == null) return null;

        IString2DataConvertor<T> converter = String2DataConvertorFactory.getConvertor(componentType);
        String[] elementResults = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            T element = data[i];
            elementResults[i] = converter.format(element, format);
        }

        String result = StringUtils.join(elementResults, ARRAY_ELEMENTS_SEPARATOR);
        return result;
    }

    /**
     * @param data <code>{@link String}</code> representation of the array.
     * @return array of elements. <code>NULL</code> if input is empty or can`t get the component type of the array.
     */
    @SuppressWarnings("unchecked")
	@Override
    public T[] parse(String data, String format) {
        if (data == null) return null;
        if (data.length() == 0) return (T[]) Array.newInstance(componentType, 0);

        String[] elementValues = StringTool.splitAndEscape(data, ARRAY_ELEMENTS_SEPARATOR,
                ARRAY_ELEMENTS_SEPARATOR_ESCAPER);

        List<Object> elements = new ArrayList<Object>();

        IString2DataConvertor<T> converter = String2DataConvertorFactory.getConvertor(componentType);
        for (String elementValue : elementValues) {
            Object element;
            if (elementValue.length() == 0) {
                element = null;
            } else {
                element = converter.parse(elementValue, format);
            }
            elements.add(element);
        }

        T[] resultArray = (T[]) Array.newInstance(componentType, elements.size());
        T[] result = elements.toArray(resultArray);
        return result;
    }
}
