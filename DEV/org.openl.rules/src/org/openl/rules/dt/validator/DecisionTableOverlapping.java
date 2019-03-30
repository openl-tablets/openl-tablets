/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.ie.constrainer.consistencyChecking.Overlapping;
import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 * 
 */
public class DecisionTableOverlapping {

    private int[] rulesIndexes;
    private ArrayOfNamedValues value;
    private Overlapping.OverlappingStatus status;

    public Overlapping.OverlappingStatus getStatus() {
        return status;
    }

    public DecisionTableOverlapping(int[] rulesIndexes,
            ArrayOfNamedValues value,
            Overlapping.OverlappingStatus status) {
        this.rulesIndexes = rulesIndexes;
        this.value = value;
        this.status = status;
    }

    public int[] getRulesIndexes() {
        return rulesIndexes;
    }

    public ArrayOfNamedValues getValues() {
        return value;
    }

    @Override
    public String toString() {
        switch (status) {
            case BLOCK:
                return String.format("Rule #%d completely blocks rule #%d. For example: %s",
                    rulesIndexes[0],
                    rulesIndexes[1],
                    value.toString());
            case PARTIAL:
                return String.format("Rule #%d partially overlaps with rule #%d. For example:  %s",
                    rulesIndexes[0],
                    rulesIndexes[1],
                    value.toString());

            case OVERRIDE:
                return String.format("Rule #%d overrides rule #%d. For example: %s",
                    rulesIndexes[1],
                    rulesIndexes[0],
                    value.toString());
        }
        return String.format("Rules with # %s overlap for values: %s", asString(rulesIndexes), value.toString());
    }

    private String asString(int[] ary) {
        StringBuilder buf = new StringBuilder(100);

        if (ary == null) {
            buf.append("null");
        } else {

            buf.append('[');
            for (int i = 0; i < ary.length; ++i) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(ary[i]);
            }
            buf.append(']');
        }
        return buf.toString();
    }

}
