/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.ss.formula;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.record.formula.AbstractFunctionPtg;
import org.apache.poi.hssf.record.formula.AddPtg;
import org.apache.poi.hssf.record.formula.ConcatPtg;
import org.apache.poi.hssf.record.formula.DividePtg;
import org.apache.poi.hssf.record.formula.EqualPtg;
import org.apache.poi.hssf.record.formula.GreaterEqualPtg;
import org.apache.poi.hssf.record.formula.GreaterThanPtg;
import org.apache.poi.hssf.record.formula.IntersectionPtg;
import org.apache.poi.hssf.record.formula.LessEqualPtg;
import org.apache.poi.hssf.record.formula.LessThanPtg;
import org.apache.poi.hssf.record.formula.MultiplyPtg;
import org.apache.poi.hssf.record.formula.NotEqualPtg;
import org.apache.poi.hssf.record.formula.OperationPtg;
import org.apache.poi.hssf.record.formula.PercentPtg;
import org.apache.poi.hssf.record.formula.PowerPtg;
import org.apache.poi.hssf.record.formula.RangePtg;
import org.apache.poi.hssf.record.formula.SubtractPtg;
import org.apache.poi.hssf.record.formula.UnaryMinusPtg;
import org.apache.poi.hssf.record.formula.UnaryPlusPtg;
import org.apache.poi.hssf.record.formula.eval.ConcatEval;
import org.apache.poi.hssf.record.formula.eval.FunctionEval;
import org.apache.poi.hssf.record.formula.eval.IntersectionEval;
import org.apache.poi.hssf.record.formula.eval.PercentEval;
import org.apache.poi.hssf.record.formula.eval.RangeEval;
import org.apache.poi.hssf.record.formula.eval.RelationalOperationEval;
import org.apache.poi.hssf.record.formula.eval.TwoOperandNumericOperation;
import org.apache.poi.hssf.record.formula.eval.UnaryMinusEval;
import org.apache.poi.hssf.record.formula.eval.UnaryPlusEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.function.FunctionMetadataRegistry;
import org.apache.poi.hssf.record.formula.functions.ArrayMode;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.functions.Function;
import org.apache.poi.hssf.record.formula.functions.FunctionBase;
import org.apache.poi.hssf.record.formula.functions.Indirect;
import org.apache.poi.ss.usermodel.ArrayFormulaEvaluatorHelper;

/**
 * This class creates <tt>OperationEval</tt> instances to help evaluate <tt>OperationPtg</tt>
 * formula tokens.
 *
 * @author Josh Micich
 */
final class OperationEvaluatorFactory {

	private static final Map<OperationPtg, Function> _instancesByPtgClass = initialiseInstancesMap();

	private OperationEvaluatorFactory() {
		// no instances of this class
	}

	private static Map<OperationPtg, Function> initialiseInstancesMap() {
		Map<OperationPtg, Function> m = new HashMap<OperationPtg, Function>(32);

		put(m, EqualPtg.instance, RelationalOperationEval.EqualEval);
		put(m, GreaterEqualPtg.instance, RelationalOperationEval.GreaterEqualEval);
		put(m, GreaterThanPtg.instance, RelationalOperationEval.GreaterThanEval);
		put(m, LessEqualPtg.instance, RelationalOperationEval.LessEqualEval);
		put(m, LessThanPtg.instance, RelationalOperationEval.LessThanEval);
		put(m, NotEqualPtg.instance, RelationalOperationEval.NotEqualEval);

		put(m, ConcatPtg.instance, ConcatEval.instance);
		put(m, AddPtg.instance, TwoOperandNumericOperation.AddEval);
		put(m, DividePtg.instance, TwoOperandNumericOperation.DivideEval);
		put(m, MultiplyPtg.instance, TwoOperandNumericOperation.MultiplyEval);
		put(m, PercentPtg.instance, PercentEval.instance);
		put(m, PowerPtg.instance, TwoOperandNumericOperation.PowerEval);
		put(m, SubtractPtg.instance, TwoOperandNumericOperation.SubtractEval);
		put(m, UnaryMinusPtg.instance, UnaryMinusEval.instance);
		put(m, UnaryPlusPtg.instance, UnaryPlusEval.instance);
		put(m, RangePtg.instance, RangeEval.instance);
		put(m, IntersectionPtg.instance, IntersectionEval.instance);
		return m;
	}

	private static void put(Map<OperationPtg, Function> m, OperationPtg ptgKey,
			Function instance) {
		// make sure ptg has single private constructor because map lookups assume singleton keys
		Constructor[] cc = ptgKey.getClass().getDeclaredConstructors();
		if (cc.length > 1 || !Modifier.isPrivate(cc[0].getModifiers())) {
			throw new RuntimeException("Failed to verify instance ("
					+ ptgKey.getClass().getName() + ") is a singleton.");
		}
		m.put(ptgKey, instance);
	}

	/**
	 * returns the OperationEval concrete impl instance corresponding
	 * to the supplied operationPtg
	 */
	public static ValueEval evaluate(OperationPtg ptg, ValueEval[] args,
			OperationEvaluationContext ec) {
		if(ptg == null) {
			throw new IllegalArgumentException("ptg must not be null");
		}
		FunctionBase func = _instancesByPtgClass.get(ptg);

		if (func == null && ptg instanceof AbstractFunctionPtg) {
			AbstractFunctionPtg fptg = (AbstractFunctionPtg)ptg;
			int functionIndex = fptg.getFunctionIndex();
			switch (functionIndex) {
				case FunctionMetadataRegistry.FUNCTION_INDEX_INDIRECT:
					func = Indirect.instance;
					break;
				case FunctionMetadataRegistry.FUNCTION_INDEX_EXTERNAL:
					func = UserDefinedFunction.instance;
					break;
				default:	
					func = FunctionEval.getBasicFunction(functionIndex);
				break;
			}
		}
		if (func != null) {
            if (func instanceof ArrayMode && ec.isInArrayFormulaContext()) {
                return evaluateInSpecialModeForArrayFormulas((ArrayMode) func, args, ec);
            } else {
                return invokeOperationInArrayContext(func, args, ec);
            }
		} else {
		    throw new RuntimeException("Unexpected operation ptg class (" + ptg.getClass().getName() + ")");
		}
	}
	
    private static ValueEval evaluateInSpecialModeForArrayFormulas(ArrayMode function, ValueEval[] ops,
            OperationEvaluationContext ec) {
        return function.evaluateInArrayFormula(ops, ec.getRowIndex(), ec.getColumnIndex());
    }

    private static ValueEval invokeOperationInArrayContext(FunctionBase func, ValueEval[] ops, OperationEvaluationContext ec) {
        boolean isArrayFormula = ec.isInArrayFormulaContext();
        ValueEval answer = ArrayFormulaEvaluatorHelper.prepareEmptyResult(func, ops, isArrayFormula);
        if (answer instanceof ArrayEval) {
            ValueEval[][] values = (ValueEval[][]) ((ArrayEval) answer).getArrayValues();
            for (int row = 0; row < values.length; row++)
                for (int col = 0; col < values[row].length; col++) {
                    ValueEval[] opsloop = ArrayFormulaEvaluatorHelper.prepareArgsForLoop(func, ops, row, col, isArrayFormula);
                    ValueEval loopresult;
                    if(func instanceof Function){
                    	loopresult = ((Function)func).evaluate(opsloop, ec.getRowIndex(), ec.getColumnIndex());
                    }
                    else {
                    	loopresult = ((FreeRefFunction)func).evaluate(opsloop, ec);
                    }
                    values[row][col] = loopresult;
                }
            return answer;
        } else {
            if(func instanceof Function){
            	return ((Function)func).evaluate(ops, ec.getRowIndex(), ec.getColumnIndex());
            }
            else {
            	return  ((FreeRefFunction)func).evaluate(ops, ec);
                }
       	
        }
    }
}
