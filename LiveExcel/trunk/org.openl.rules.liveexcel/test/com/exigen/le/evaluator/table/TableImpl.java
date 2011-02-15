/**
 *
 * Fake class - need to replace by real
 */
package com.exigen.le.evaluator.table;

import java.util.List;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

import com.exigen.le.evaluator.table.LETableFactory.TableElement;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.emulator.TableEmulator;

/**
 * @author vabramovs
 *
 */
public class TableImpl extends TableElement {

	TableImpl(String connectionURL, List<TableDesc> tds) {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */

	@Override
	public Object calculate(String tableName, Object[] params){
		return TableEmulator.calculate(params);
	}

}
