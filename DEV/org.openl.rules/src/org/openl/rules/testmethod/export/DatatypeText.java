package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.IOpenClass;

/**
 * Writes the value of a datatype as text, field by field.
 *
 * <p>The text reads as the {@code toString()} of the datatype, {@code FinancialData{ totalAssets=0.0001 }}, but
 * every field is written as the cells of the parameters write it. A number keeps its plain notation, so 0.0001 is
 * not written as 1.0E-4. A field of a datatype is written the same way, and an array field lists its elements in
 * brackets.
 *
 * <p>A value of another type is written by its formatter. So is a value of a datatype that only inherits the type
 * it is declared with: the declared type does not know all its fields.
 *
 * @author Yury Molchan
 */
final class DatatypeText {

    private DatatypeText() {
    }

    /**
     * The text of a cell holding a value of the given type. An array of datatype values lists the text of each
     * element, separated by commas, as an array of other values is written.
     */
    static String format(Object value, IOpenClass type) {
        if (value.getClass().isArray() && type.isArray() && type.getComponentClass() instanceof DatatypeOpenClass) {
            return elements(value, type.getComponentClass(), ",");
        }
        return single(value, type);
    }

    private static String field(@Nullable Object value, IOpenClass type) {
        if (value != null && value.getClass().isArray() && type.isArray()) {
            return "[" + elements(value, type.getComponentClass(), ", ") + "]";
        }
        return single(value, type);
    }

    private static String single(@Nullable Object value, IOpenClass type) {
        return isDatatype(value, type) ? fields(value, type) : FormattersManager.format(value);
    }

    private static boolean isDatatype(@Nullable Object value, IOpenClass type) {
        return value != null && type instanceof DatatypeOpenClass && type.getInstanceClass() == value.getClass();
    }

    private static String fields(Object value, IOpenClass type) {
        var text = new StringBuilder(type.getName()).append('{');
        for (var field : type.getFields()) {
            text.append(' ').append(field.getName()).append('=').append(field(field.get(value, null), field.getType()));
        }
        return text.append(" }").toString();
    }

    private static String elements(Object array, IOpenClass componentType, String separator) {
        var text = new StringJoiner(separator);
        for (var i = 0; i < Array.getLength(array); i++) {
            text.add(field(Array.get(array, i), componentType));
        }
        return text.toString();
    }
}
