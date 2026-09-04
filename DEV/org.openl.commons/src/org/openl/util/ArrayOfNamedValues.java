/**
 * Created Feb 11, 2007
 */
package org.openl.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class ArrayOfNamedValues {

    private final String[] names;
    private final Object[] values;

    public String getName(int i) {
        return names[i];
    }

    public Object getValue(int i) {
        return values[i];
    }

    public int size() {
        return names.length;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var size = size();

        for (var i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getName(i)).append(" = ");
            var value = getValue(i);
            if (value instanceof Date) {
                var dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                sb.append(dateFormat.format(value));
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }

}
