package org.openl.rules.liveexcel.formula.lookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.formula.FunctionParam;
import org.openl.rules.liveexcel.ranges.DoubleRangeParser;
import org.openl.rules.liveexcel.ranges.RangeEval;

/**
 * Resolves types of input parameters(dimensions).
 * 
 * @author PUdalau
 */
public class LookupTypeResolver {
    private LiveExcelLookup lookup;

    public LookupTypeResolver(LiveExcelLookup lookup) {
        this.lookup = lookup;
    }

    private static Class<?> resolveTypeByValue(ValueEval value) {
        if (value instanceof NumberEval) {
            return RangeEval.class;
        } else if (value instanceof BoolEval) {
            return BoolEval.class;
        } else if (value instanceof StringEval) {
            if (DoubleRangeParser.isRange(((StringEval) value).getStringValue())) {
                return RangeEval.class;
            } else {
                return String.class;
            }
        } else {
            return Object.class;
        }
    }

    private Class<?> getTypeOfParam(int paramIndex) {
        Class<?> type = null;
        int paramCount = lookup.getLookupData().getWidth() - 1;
        for (int i = 0; i < lookup.getLookupData().getHeight(); i++) {
            if (!lookup.getLookupData().isBlank(paramCount - 1 - paramIndex, i)) {
                if (type == null) {
                    type = resolveTypeByValue(lookup.getLookupData().getValue(paramCount - 1 - paramIndex, i));
                } else {
                    Class<?> newType = resolveTypeByValue(lookup.getLookupData().getValue(paramCount - 1 - paramIndex,
                            i));
                    if (newType != type) {
                        // if values of some dimension are different than type
                        // of dimension will be String
                        type = StringEval.class;
                        break;
                    }
                }
            }
        }
        return type;
    }

    private void convertParamToRange(int paramIndex) {
        int paramCount = lookup.getLookupData().getWidth() - 1;

        for (int i = 0; i < lookup.getLookupData().getHeight(); i++) {
            if (!lookup.getLookupData().isBlank(paramCount - 1 - paramIndex, i)) {
                ValueEval previousValue = lookup.getLookupData().getValue(paramCount - 1 - paramIndex, i);
                if (!(previousValue instanceof RangeEval)) {
                    RangeEval convertedValue = new DoubleRangeParser().parse(((StringValueEval) previousValue)
                            .getStringValue());
                    lookup.getLookupData().setValue(paramCount - 1 - paramIndex, i, convertedValue);
                }
            }
        }
    }

    /**
     * Resolves types of all input parameters.
     * 
     * @param description Description of lookup.
     */
    public void initParameters(String description) {
        lookup.setReturnCell(new FunctionParam(description, null));
        List<FunctionParam> params = new ArrayList<FunctionParam>();
        for (int i = 0; i < lookup.getLookupData().getWidth() - 1; i++) {
            Class<?> paramType = getTypeOfParam(i);
            params.add(new FunctionParam("", null, paramType));
            if (paramType == RangeEval.class) {
                convertParamToRange(i);
            }
            // TODO: converting to StringEval for dimension with different
            // values
        }
        lookup.setParameters(params);
    }

}
