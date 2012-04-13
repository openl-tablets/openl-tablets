/**
 * 
 */
package com.exigen.le;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

import com.exigen.le.evaluator.DataPool;
import com.exigen.le.evaluator.LiveExcelEvaluator;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * @author vabramovs
 *
 */
public class LE_Value {
	private static final Log LOG = LogFactory.getLog(LE_Value.class);

		public class  Type {
			public static final int BOOLEAN = 0;
			public static final int DOUBLE = 1;
			public static final int NUMERIC = 1;
			public static final int STRING = 2;
			public static final int ERROR = 4;
			public static final int DATE = 6;
			public static final int VALUE_HOLDER = 7;
			public static final int ARRAY = 8;
			
		}
		public class  TypeString {
		
			public static final String BOOLEAN = "BOOLEAN";
			public static final String NUMERIC = "NUMERIC";
			public static final String STRING = "STRING";
			public static final String ERROR = "ERROR";
			public static final String DATE = "DATE";
		}
			
		private static final long  mSecInDay = 1000 * 60 * 60 * 24;
		
		String value;   // For Date this field keeps "java-based" Date value
		int type;
		ValueHolder object;
		LE_Value[][] array;
		
		public void  setValue(String value){
			this.value = value;
			setType(Type.STRING);		
		}
		
		public void  setValue(double value){
			this.value = value+"";
			setType(Type.DOUBLE);
			
		}
		public void  setValue(boolean value){
			this.value = value+"";
			setType(Type.BOOLEAN);
			
		}
		public void  setValue(Date value){
			this.value =value.getTime()+"";
			setType(Type.DATE);
			
		}

		public void setValue(ValueHolder object){
			this.object = object;
			setType(Type.VALUE_HOLDER);
			
		}
		public void setValue(LE_Value[][] array){
			this.array = array;
			setType(Type.ARRAY);
			
		}
		public void  setErrorValue(int value){
			this.value = value+"";
			setType(Type.ERROR);
			
		}
		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @return the java Date
		 */
		public Date getDateValue() {
			if(type == Type.DATE){
				return new Date(Long.parseLong(value));
			}
			else
				throw new RuntimeException("Value '"+value+"' has type:"+type+",not DATE");
		}
		
		/**
		 * @return the Value Holder object
		 */
		public ValueHolder getValueHolder() {
			if(type == Type.VALUE_HOLDER){
				return object;
			}
			else
				throw new RuntimeException("Value '"+value+"' has type:"+type+",not VALUE_HOLDER");
		}
		/**
		 * @return the LE_Value array
		 */
		public LE_Value[][] getArray() {
			if(type == Type.ARRAY){
				return array;
			}
			else
				return new LE_Value[][]{{this}};
//				throw new RuntimeException("Value '"+value+"' has type:"+type+",not ARRAY");
		}
		/**
		 * @return the Excel Date
		 */
		public double getExcelDateValue() {
			if(type == Type.DATE){
				return Double.parseDouble(value);
			}
			else
				throw new RuntimeException("Value '"+value+"' has type:"+type+",not DATE");
			
		}
		/**
		 * 
		 * @return the type
		 */
		public int getType() {
			return type;
		}
		
		/**
		 * @param type
		 */
		public void setType(int type) {
			this.type = type;
		}
		/**
		 * Convert LE_Value to POI ValueEval
		 * @param value
		 * @return
		 */
		static public ValueEval createValueEval(LE_Value value){
			String strValue = value.getValue();
			switch(value.getType()){
			case Type.BOOLEAN:
					return Boolean.parseBoolean(strValue)?BoolEval.TRUE:BoolEval.FALSE;
			case Type.DOUBLE:
			case Type.DATE:
				
				return new NumberEval(Double.parseDouble(strValue));
			case Type.STRING:
				return new StringEval(strValue);
			case Type.ERROR:
				return ErrorEval.valueOf(Integer.parseInt(strValue));
			case Type.VALUE_HOLDER:
				return LiveExcelEvaluator.createEvalForObject(value.getValueHolder(),null,ThreadEvaluationContext.getDataPool());
			case Type.ARRAY:
				LE_Value[][] array = value.getArray();
				ValueEval[][] values = new ValueEval[array.length][array[0].length];
				for(int i=0;i<array.length;i++){
					for(int j=0;j<array.length;j++){
						values[i][j]=createValueEval(array[i][j]);
					}
				}
				return new 	ArrayEval(values);
 
			}
			return ErrorEval.VALUE_INVALID;
		}
		
		/**
		 * Convert POI ValueEval to LE_Value
		 * @param poiValue
		 * @return
		 */
		static public LE_Value fromValueEval(ValueEval poiValue){
			LE_Value answer = new LE_Value();
			
			if(poiValue instanceof NumberEval)
			{
				
				answer.setValue(((NumberEval)poiValue).getNumberValue());
			}
			else if(poiValue instanceof BoolEval)
			{
				answer.setValue(((BoolEval)poiValue).getBooleanValue());
			}
			else if(poiValue instanceof StringEval)
			{
				String value = ((StringEval)poiValue).getStringValue();
				if(DataPool.isOurUUID(value)){
					try {
						ValueHolder object = (ValueHolder)ThreadEvaluationContext.getDataPool().get(value);
						answer.setValue(object);
					} catch (java.lang.ClassCastException e) {
						String msg = "Object "+value+ "does not support ValueHolder interface";
						LOG.error(msg);
						throw new RuntimeException(msg,e);
					}
				}
				else{
					answer.setValue(value);
				}
			}
			else if(poiValue instanceof ArrayEval)
			{
				ValueEval[][] array =((ArrayEval)poiValue).getArrayValues();
				LE_Value[][] result = new LE_Value[array.length][array[0].length]; 
				for(int i=0;i<array.length;i++){
					for(int j=0;j<array[0].length;j++){
						result[i][j]=fromValueEval(array[i][j]);
					}
					
				}
				answer.setValue(result);
			}
			else if(poiValue instanceof ErrorEval)
			{
				answer.setErrorValue(((ErrorEval)poiValue).getErrorCode());
			}
			return answer;
		}
		/**
		 * Get LE_Value from cell
		 * @param c
		 * @return
		 */
		static public LE_Value fromCell(Cell c){
			LE_Value answer = new LE_Value();
			
			int type = c.getCellType();
			switch (type){
			case Cell.CELL_TYPE_NUMERIC:
				if(DateUtil.isCellDateFormatted(c))
				{
					
//					answer.setExcelDateValue(c.getNumericCellValue());
					answer.setValue(c.getDateCellValue());
				}	
				else
					answer.setValue(c.getNumericCellValue());
				break;
			case Cell.CELL_TYPE_BOOLEAN:
					answer.setValue(c.getBooleanCellValue());
				break;
			case Cell.CELL_TYPE_BLANK:
				answer.setValue("");
			break;
			case Cell.CELL_TYPE_STRING:
				String value = c.getStringCellValue();
				if(DataPool.isOurUUID(value)){
					try {
						ValueHolder object = (ValueHolder)ThreadEvaluationContext.getDataPool().get(value);
						answer.setValue(object);
					} catch (java.lang.ClassCastException e) {
						String msg = "Object "+value+ "does not support ValueHolder interface";
						LOG.error(msg);
						throw new RuntimeException(msg,e);
					}
				}
				else{
					answer.setValue(value);
				}
			break;
			case Cell.CELL_TYPE_ERROR:
				answer.setErrorValue(c.getErrorCellValue());
			break;
			}
					
		   return answer;
		}
}
