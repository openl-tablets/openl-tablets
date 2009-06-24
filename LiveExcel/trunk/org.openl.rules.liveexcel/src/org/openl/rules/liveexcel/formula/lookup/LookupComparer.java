package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.ranges.RangeEval;

public class LookupComparer {

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

    public static boolean isRangeMatched(RangeEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof NumberEval
                && matcherParameter.contains(((NumberEval) valueToMatch).getNumberValue())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isStringMatched(StringEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof StringValueEval
                && matcherParameter.getStringValue().equals(((StringValueEval) valueToMatch).getStringValue())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNumberMatched(NumberEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof NumberEval
                && Double.compare(matcherParameter.getNumberValue(), ((NumberEval) valueToMatch).getNumberValue()) == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isBooleanMatched(BoolEval matcherParameter, ValueEval valueToMatch) {
        if (valueToMatch instanceof BoolEval
                && matcherParameter.getBooleanValue() == ((BoolEval) valueToMatch).getBooleanValue()) {
            return true;
        } else {
            return false;
        }
    }
}
