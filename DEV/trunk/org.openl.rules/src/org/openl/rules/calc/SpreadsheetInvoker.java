package org.openl.rules.calc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link Spreadsheet}.
 * 
 * @author DLiauchuk
 * 
 */
public class SpreadsheetInvoker extends RulesMethodInvoker {

	private final Log log = LogFactory.getLog(SpreadsheetInvoker.class);
	protected Object[][] preFetchedResult;

	public SpreadsheetInvoker(Spreadsheet spreadsheet) {
		super(spreadsheet);
		this.preFetchedResult = preFetchResult(spreadsheet);
	}

	@Override
	public Spreadsheet getInvokableMethod() {
		return (Spreadsheet) super.getInvokableMethod();
	}

	public boolean canInvoke() {
		return getInvokableMethod().getResultBuilder() != null;
	}

	public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
		SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(
				getInvokableMethod(), (IDynamicObject) target, params, env,
//				(Object[][])null);
				preFetchedResult);
		return getInvokableMethod().getResultBuilder().makeResult(res);
	}

	public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
		Tracer tracer = Tracer.getTracer();

		Object result = null;

		SpreadsheetTraceObject traceObject = (SpreadsheetTraceObject) getTraceObject(params);
		tracer.push(traceObject);

		try {
			SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(
					getInvokableMethod(), (IDynamicObject) target, params, env,
					traceObject);

			result = getInvokableMethod().getResultBuilder().makeResult(res);
			traceObject.setResult(result);
		} catch (RuntimeException e) {
			traceObject.setError(e);
                        log.error("Error when tracing Spreadsheet table", e);
			throw e;
		} finally {
			tracer.pop();
		}
		return result;
	}

	/**
	 * Creates a result with constant values that are populated
	 * 
	 * @param spreadsheet
	 */
	protected Object[][] preFetchResult(Spreadsheet spreadsheet) {
		SpreadsheetCell[][] cc = spreadsheet.getCells();
		Object[][] res = new Object[cc.length][cc[0].length];

		for (int i = 0; i < cc.length; i++) {
			SpreadsheetCell[] row = cc[i];
			for (int j = 0; j < row.length; j++) {
				SpreadsheetCell cell = row[j];
				switch (cell.getKind()) {
				case EMPTY:
					res[i][j] = null;
					break;
				case VALUE:
					res[i][j] = cell.getValue();
					break;
				case METHOD:
					res[i][j] = SpreadsheetResultCalculator.NEED_TO_CALCULATE_VALUE;
					break;
					
				}

			}

		}

		return res;

	}

}
