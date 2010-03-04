package org.openl.util;

public class ArrayUtils {

    /**
     * Checks that array is not empty.
     * 
     * Used the following rules for checking:
     *   isEmpty (null) -> true;
     *   isEmpty (new Object[] {}) -> true;
     *   isEmpty (new Object[] {null, null, ....}) -> true;
     *   isEmpty (new Object[] {null, ..., <not null value>, ...}) -> false.
     *   
     * @param array array
     * @return true if array is empty; false - otherwise
     */
    public static boolean isEmpty(Object[] array) {

        if (array != null) {

            for (Object element : array) {
                if (element != null) {
                    return false;
                }
            }
        }

        return true;
    }

}
