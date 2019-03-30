package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: DTChecker is the interface to execute completeness and overlappings monitoring
 * for the given Decision Table</p>
 * <p>Decision Table has to relate every state of an object with an appropriate action. So there could
 * arise two kind  of problems: the first is incompletness, in other words there could exist points in the
 * state of spaces not covered by any rule and the second is overlapping. Overlapping is the situation in
 * which the same state of an object could be handled by two or more rules, possibly implying
 * opposite actions to be performed. Such situations are rarely being the consequences of the well planned
 * rule policy so the user would rather be notified. </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.HashMap;
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
    public List<Uncovered> checkCompleteness();

    /**
     * Looks for overlapping rules
     *
     * @return Vector of <code>Overlapping</code>
     * @see Overlapping
     */
    public List<Overlapping> checkOverlappings();

    public CDecisionTable getDT();

    /**
     * @param dtable the Decision Table to be checked
     */
    public void setDT(CDecisionTable dtable);

}