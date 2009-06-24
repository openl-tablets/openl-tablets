package org.openl.rules.liveexcel.ranges;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

public interface RangeEval extends ValueEval {
    boolean contains(double number);
}
