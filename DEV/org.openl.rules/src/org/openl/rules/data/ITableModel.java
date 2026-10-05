/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public interface ITableModel {

    ColumnDescriptor[] getDescriptors();

    Class<?> getInstanceClass();

    String getName();

    IOpenClass getType();

    Object newInstance();

    boolean hasColumnTitleRow();

    ColumnDescriptor getDescriptor(int idx);

    int getColumnCount();

    /**
     * The column a reference reads a row of the table by when it names no column: the key of the table.
     *
     * <p>The first field of the table keys it, a {@code _PK_} column written first among them: a {@code _PK_} column
     * written after another field keys nothing. A table whose first column is the key of another level is keyed by its
     * first field.
     *
     * @return the index of the column
     */
    default int getKeyColumnIndex() {
        var descriptors = getDescriptors();
        if (descriptors.length == 0) {
            return 0;
        }
        var first = descriptors[0];
        if (first.isPrimaryKey()) {
            return first.getColumnIdx();
        }
        // The first column is the primary key of another level: the first descriptor keys the table.
        var firstColumn = getDescriptor(0);
        return firstColumn != null && firstColumn.isPrimaryKey() ? first.getColumnIdx() : 0;
    }
}
