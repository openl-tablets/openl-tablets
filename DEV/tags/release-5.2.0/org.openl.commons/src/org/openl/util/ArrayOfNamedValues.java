/**
 * Created Feb 11, 2007
 */
package org.openl.util;

/**
 * @author snshor
 */
public class ArrayOfNamedValues {

    String[] names;
    Object[] values;

    public ArrayOfNamedValues(String[] names, Object[] values) {
        this.names = names;
        this.values = values;
    }

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
        StringBuilder sb = new StringBuilder();
        int size = size();

        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(getName(i)).append("=").append(getValue(i));

        }

        return sb.toString();
    }

}
