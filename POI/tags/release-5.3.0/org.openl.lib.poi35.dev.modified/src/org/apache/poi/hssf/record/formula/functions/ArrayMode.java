/**
 * 
 */
package org.apache.poi.hssf.record.formula.functions;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * @author zsulkins
 * Interface for those functions that behaves differently in array formula 
 */
public interface ArrayMode {

	ValueEval evaluateInArrayFormula(ValueEval[] args, int srcRowIndex, short srcColumnIndex);

}
