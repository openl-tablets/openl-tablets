package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.ranges.DoubleRangeParser;
import org.openl.rules.liveexcel.ranges.RangeEval;

/**
 * Class that compares values represented as {@link ValueEval} types.
 * 
 * @author PUdalau
 */
public class LookupComparer {

    /**
     * Attempts to match the value against the parameter.
     * 
     * @param matcherParameter Matcher parameter.
     * @param valueToMatch Another value, that probably matched .
     * @return <code>true</code> if value matches parameter.
     */
    public static boolean isMatched(ValueEval matcherParameter, ValueEval valueToMatch) {
        if (matcherParameter instanceof RangeEval) {
            return isRangeMatched((RangeEval) matcherParameter, valueToMatch);
        } else if (matcherParameter instanceof StringEval) {
            return isStringMatched((StringEval) matcherParameter, valueToMatch);
        } else if (matcherParameter instanceof NumberEval) {
            return isNumberMatched((NumberEval) matcherParameter, valueToMatch);
        } else if (matcherParameter instanceof BoolEval) {
            return isBooleanMatched((BoolEval) matcherParameter, valueToMatch);
        }
        return false;
    }

    /**
     * Checks if the value belongs to interval.
     * 
     * @param matcherParameter Range that probably contains interval.
     * @param valueToMatch Value to check.
     * @return <code>true</code> if value belong to range.
     */
    public static boolean isRangeMatched(RangeEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof StringValueEval) {
            String stringRepresentation = ((StringValueEval) valueToMatch).getStringValue();
            if (DoubleRangeParser.isRange(stringRepresentation)) {
                return matcherParameter.contains(DoubleRangeParser.parseNumber(stringRepresentation));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks if the value in string representation equals matcher string.
     * 
     * @param matcherParameter String matcher
     * @param valueToMatch value to check.
     * @return <code>true</code> if value equals string matcher.
     */
    public static boolean isStringMatched(StringEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof StringValueEval
                && matcherParameter.getStringValue().trim().equals(((StringValueEval) valueToMatch).getStringValue().trim())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if two values are equal.
     * 
     * @param matcherParameter Number value.
     * @param valueToMatch Another value.
     * @return <code>true</code> if values are equal.
     */
    public static boolean isNumberMatched(NumberEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof NumberEval
                && Double.compare(matcherParameter.getNumberValue(), ((NumberEval) valueToMatch).getNumberValue()) == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if two values are equal.
     * 
     * @param matcherParameter Boolean value.
     * @param valueToMatch Another value.
     * @return <code>true</code> if values are equal.
     */
    public static boolean isBooleanMatched(BoolEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof BoolEval
                && matcherParameter.getBooleanValue() == ((BoolEval) valueToMatch).getBooleanValue()) {
            return true;
        } else {
            return false;
        }
    }
}
