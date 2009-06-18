package org.openl.rules.liveexcel;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEvalBase;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.formula.FunctionParam;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class TypeUtils {

    public static IOpenClass getParameterClass(FunctionParam functionParam) {
        try {
            ValueEval innerValueEval = ((RefEvalBase) functionParam.getParamCell()).getInnerValueEval();
            if (innerValueEval instanceof NumberEval) {
                return JavaOpenClass.DOUBLE;
            } else if (innerValueEval instanceof BoolEval) {
                return JavaOpenClass.BOOLEAN;
            } else if (innerValueEval instanceof StringEval) {
                return JavaOpenClass.STRING;
            }
            return JavaOpenClass.OBJECT;
        } catch (Exception e) {
            return JavaOpenClass.OBJECT;
        }
    }

    public static String convertName(String name) {
        if (name != null) {
            String[] parts = name.split(" ");
            parts[0] = StringUtils.uncapitalize(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                parts[i] = StringUtils.capitalize(parts[i]);
            }
            return StringUtils.join(parts);
        }
        return null;
    }

}
