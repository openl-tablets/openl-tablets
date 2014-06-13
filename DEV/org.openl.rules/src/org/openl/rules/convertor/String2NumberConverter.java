package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

/**
 * A base converter class which implements logic for parsing and formatting Java's numbers.
 * It uses <code>NumberFormatHelper</code>.
 *
 * @param <T> type of a number
 * @author Yury Molchan
 * @see org.openl.rules.convertor.NumberFormatHelper
 */
abstract class String2NumberConverter<T extends Number> implements IString2DataConvertor {

    private final NumberFormatHelper formatHelper;

    String2NumberConverter() {
        formatHelper = new NumberFormatHelper();
    }

    String2NumberConverter(String defaultFormat) {
        formatHelper = new NumberFormatHelper(defaultFormat);
    }

    @Override
    public String format(Object data, String format) {
        return formatHelper.format((Number) data, format);
    }

    @Override
    public T parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;
        Number n = formatHelper.parse(data, format);
        return convert(n, data);
    }

    /**
     * Converts a number to the primitive type
     *
     * @param number a number
     * @param data   a parsed string to the number
     * @return a wrapped primitive type
     */
    abstract T convert(Number number, String data);
}
