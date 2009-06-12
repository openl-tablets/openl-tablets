package org.openl.rules.lang.xls;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class LiveExcelMethod extends AMethod {
	
	private ParsedDeclaredFunction declaredFunction;

	public LiveExcelMethod(IOpenMethodHeader header,ParsedDeclaredFunction declaredFunction) {
		super(header);
		this.declaredFunction = declaredFunction;
	}

	@Override
	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
		ValueEval[] args = new ValueEval[params.length];
		for (int i = 0; i < params.length; i ++) {
			Object param = params[i];
			if (param instanceof Number) {
				args[i] = new NumberEval(((Number)param).doubleValue());
			} else if (param instanceof Boolean) {
				args[i] = BoolEval.valueOf((Boolean)param);
			} else if (param instanceof String) {
				args[i] = new StringEval((String)param);
			}
		}
		ValueEval evaluate = declaredFunction.evaluate(args, null, 0, 0, 0);
		if (evaluate instanceof NumberEval) {
			return ((NumberEval)evaluate).getNumberValue();
		} else if (evaluate instanceof StringEval) {
			return ((StringEval)evaluate).getStringValue();
		} else if (evaluate instanceof BoolEval) {
			return ((BoolEval)evaluate).getBooleanValue();
		}
		return null;
	}

}
