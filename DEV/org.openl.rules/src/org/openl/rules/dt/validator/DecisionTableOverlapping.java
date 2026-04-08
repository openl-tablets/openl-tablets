/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.ie.constrainer.consistencyChecking.Overlapping;
import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 */
public class DecisionTableOverlapping {

    private final int[] rulesIndexes;
    private final ArrayOfNamedValues value;
    private final Overlapping.OverlappingStatus status;

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
        return switch (status) {
            case BLOCK -> "Rule #%d completely blocks rule #%d. For example: %s".formatted(
                        rulesIndexes[0],
                        rulesIndexes[1],
                        value.toString());
            case PARTIAL -> "Rule #%d partially overlaps with rule #%d. For example:  %s".formatted(
                        rulesIndexes[0],
                        rulesIndexes[1],
                        value.toString());

            case OVERRIDE -> "Rule #%d overrides rule #%d. For example: %s".formatted(
                        rulesIndexes[1],
                        rulesIndexes[0],
                        value.toString());
        };
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
