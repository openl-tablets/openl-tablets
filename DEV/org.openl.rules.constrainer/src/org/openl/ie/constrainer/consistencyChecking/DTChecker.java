package org.openl.ie.constrainer.consistencyChecking;

import java.util.List;

import org.openl.ie.constrainer.IntExpArray;

public interface DTChecker {
    static class Utils {

        /**
         * @param solution
         * @return
         */
        public static String[] IntExpArray2Names(IntExpArray solution) {
            String[] names = new String[solution.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = solution.get(i).name();
            }
            return names;
        }

        /**
         * @param solution
         * @return
         */
        public static int[] IntExpArray2Values(IntExpArray solution) {
            int[] values = new int[solution.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = solution.get(i).max();
            }
            return values;
        }
    }

    /**
     * Performs check of the completeness of the given rule's system
     *
     * @return Vector of points in the state of space not covered by any rules. Points are represented by objects of
     *         type <code>Uncovered</code>
     * @see Uncovered
     */
    List<Uncovered> checkCompleteness();

    /**
     * Looks for overlapping rules
     *
     * @return Vector of <code>Overlapping</code>
     * @see Overlapping
     */
    List<Overlapping> checkOverlappings();

    CDecisionTable getDT();

    /**
     * @param dtable the Decision Table to be checked
     */
    void setDT(CDecisionTable dtable);

}