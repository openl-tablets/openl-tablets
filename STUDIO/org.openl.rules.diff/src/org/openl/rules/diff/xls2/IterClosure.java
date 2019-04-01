package org.openl.rules.diff.xls2;

/**
 * Helper interface.
 *
 * @author Aleh Bykhavets
 *
 */
public interface IterClosure {
    /**
     * Check whether tables are met some criteria and are moving into other place.
     *
     * @param table1
     * @param table2
     * @return true -- remove tables from list
     */
    boolean remove(XlsTable table1, XlsTable table2);
}
