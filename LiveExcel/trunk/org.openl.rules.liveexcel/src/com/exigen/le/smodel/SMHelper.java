/**
 * 
 */
package com.exigen.le.smodel;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.usermodel.ArrayFormulaEvaluatorHelper;


import com.exigen.le.LE_Value;
import com.exigen.le.smodel.Function.FunctionArgument;
import com.exigen.le.smodel.accessor.CollectionValueHolder;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * Helper to manipulate with Service Model
 * @author vabramovs
 *
 */
public class SMHelper {
	private static final Log LOG = LogFactory.getLog(SMHelper.class);
	private static final DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);

	
	/**
	 * Print out model structure
	 * @param out
	 * @param prefix 
	 * @param node
	 */
	static public void printoutStructure(PrintStream out,String prefix,List<Type> nodes){
		List<Type> referenced = new ArrayList<Type>();
		referenced.addAll(nodes);
		Vector<Type> handled = new Vector<Type>();
		
		while(referenced.size()>0){
			if(!handled.contains(referenced.get(0))){
				out.println(prefix+referenced.get(0).getName()+":");
			}
			printoutStructureInt(out, prefix, referenced.get(0), referenced,handled);
			referenced.remove(0);
			
			
		}
	}
	static private void printoutStructureInt(PrintStream out,String prefix,Type node, List<Type> referenced,Vector<Type> handled){
		if(handled.contains(node)){
			return;
		}
		handled.add(node);
		prefix = prefix+"\t";
		String nextprefix = prefix+"\t";
		for(Property child:node.getChilds()){
			String collection = child.isCollection()?"[]":"";
			if(child.getType().isComplex()){
				if(child.isEmbedded()){
					out.println(prefix+child.getType().getName()+collection+" "+child.getName()+":");
					printoutStructureInt(out,nextprefix,child.getType(),referenced,handled);
				}
				else {
					out.println(prefix+child.getType().getName()+collection+" "+child.getName());
					if(!referenced.contains(child.getType()) )
						referenced.add(child.getType());
				}
			}
			else{
				out.println(prefix+child.getType().getName()+collection+" "+child.getName());
			}
		}

	}
	/**
	 * Print out values of object(possible complex)
	 * @param out
	 * @param prefix
	 * @param object
	 */
	public static void printoutValues(PrintStream out, String prefix,
			Object object) {
		prefix = prefix+"\t";
		Type model = ((ValueHolder)object).getModel();
	    for(Property child : model.getChilds()){
				if(child.isCollection() ){ // Array
					try {
						Object array = ((ValueHolder)object).getValue(child.getName());
						if(array instanceof CollectionValueHolder){
							for(int index =0; index <((CollectionValueHolder)array).size() ;index++){
								printoutValue(out,prefix,child,array, index);
							}
						}
						else
						{	
							if(array instanceof List<?>){
								for(int index =0; index <((List<?>)array).size() ;index++){
									printoutValue(out,prefix,child,array, index);
								}
								
							}
							else{
								for(int index =0; index <Array.getLength(array) ;index++){
									printoutValue(out,prefix,child,array, index);
								}
								
							}
							
						}
							
					} catch (Exception e) {
						LOG.warn("Could not get value for "+child.getName(),e);
					}
				}
				else {   // Scalar 
						printoutValue(out,prefix,child,((ValueHolder)object).getValue(child.getName()), -1);
				}
	        }
	    }	
		
	private static void printoutValue(PrintStream out, String prefix,Property field,Object object,int index){
		String elem = "";
		if(index != (-1)){
			elem="["+index+"]";
		}
		if(object == null){
			out.println(prefix+field.getType().getName()+" "+field.getName()+elem+" - was not determined");
			return;	
		}
		if(field.getType().isComplex()){ // Complex type
			out.println(prefix+field.getType().getName()+" "+field.getName()+elem+":");
			Object node;
			try {
				if(index ==(-1)){
					node = object;
				}
				else {
					node = ((CollectionValueHolder)object).getValue(index);
				}
				printoutValues(out, prefix, (ValueHolder)node);
			} catch (Exception e) {
				e.printStackTrace();
			}	
		
		}
		else { // Primitive type
			String valueStr="";
			Object value=null;
			try {
				if(index == (-1)){ // Scalar
					value = object;
				}
				else {  // Element of array 
					if(object instanceof CollectionValueHolder){
						
					}
					else if(object instanceof List) {
						value = ((List)object).get(index);
					}
					else{
						value = Array.get(object, index);
					}
				}	
				if(field.getType().equals(Primary.DATE.getType())){
					if(value instanceof Calendar){
						Date date = ((Calendar)value).getTime();
						valueStr=df.format(date);
					}
					if(value instanceof Double){
						Calendar calendar = new GregorianCalendar();
						calendar.setTimeInMillis((((Double)value).longValue()));
						Date date = (calendar).getTime();
						valueStr=df.format(date);
					}
				}
				else{
					valueStr=value.toString();
				}
				out.println(prefix+field.getType().getName()+" "+field.getName()+elem+"="+valueStr);
			} catch (Exception e) {
				LOG.warn("Could not get value for "+field.getName(),e);
			}
			
		}
	}
	/**
	 * Print out function description
	 * @param out
	 * @param prefix
	 * @param func
	 */
	public static void printoutFunction(PrintStream out,String prefix, 
			Function func) {
		String type = "Undefined";
		if(func.getReturnType()!= null){
			type = func.getReturnType().getName();
		}
		out.println(prefix+type+"(["+func.getExcel()+"]"+func.getSheet()+
				"!"+func.obtainReturnSpace().toString()+") "+ func.getName()+":"+func.getFunctionDescription());
		String argPrefix = prefix+"\t";
		for(FunctionArgument arg: func.getArguments()){
			String input = "Undefined";
			if(arg.obtainInput()!= null){
				input = arg.obtainInput().toString();
			}
			String desc = "Undefined";
			if(arg.getDescription()!= null){
				desc = arg.getDescription();
			}
			String argtype = "Undefined";
			if(arg.getType() != null){
				argtype = arg.getType().getName();
			}
			out.println(argPrefix+"-"+argtype+"("+input+")"+
					" "+desc);
		}
		
	}
	/** Print out descriptions of all functions
	 * @param out
	 * @param prefix
	 * @param funcs
	 */
	public static void printoutFunctions(PrintStream out,String prefix, 
			List<Function> funcs) {
		out.println( prefix + "Functions:");
		String newPrefix = "\t"+ prefix;
		for(Function func:funcs){
			printoutFunction(out,newPrefix, func);
		}
	}
	/**
	 * Print out table description
	 * @param out
	 * @param prefix
	 * @param table
	 */
	public static void printoutTable(PrintStream out, String prefix,
			TableDesc table) {
		out.println(table.toString());
		
	}
	
	/**
	 * Print out descriptions of all tables
	 * @param out
	 * @param prefix
	 * @param tables
	 */
	public static void printoutTables(PrintStream out, String prefix,
			List<TableDesc> tables) {
		out.println( prefix + "Tables:");
		String newPrefix = "\t"+ prefix;
		for(TableDesc table:tables){
			printoutTable(out, newPrefix, table);
		}
		
	}
	/**
	 * Convert value of object (possible complex) to String
	 * @param value
	 * @return
	 */
	public static String valueToString(Object value){
		if(value instanceof ValueHolder){
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(bao ); 
			SMHelper.printoutValues(out,"",value);
			return bao.toString();
		}
		else if (value instanceof LE_Value) {
			if(((LE_Value)value).getType()== LE_Value.Type.ARRAY){
				String answer = "";
				LE_Value[][] array =((LE_Value)value).getArray();
				for(int i = 0; i<array.length;i++){
					for(int j = 0; j<array[0].length;j++){
						answer = answer+"Element ["+i+"]["+j+"="+valueToString(array[i][j])+"\n";
					}
				}
				return answer;
			}
			else{
				if((((LE_Value)value).getType()== LE_Value.Type.VALUE_HOLDER))
					return valueToString(((LE_Value)value).getValue());
				else {
					return ((LE_Value)value).getValue();
				}
			}
		}
		else if (value instanceof ValueEval) {
			if(value instanceof ArrayEval){
				String answer = "";
				ValueEval[][] array =((ArrayEval)value).getArrayValues();
				for(int i = 0; i<array.length;i++){
					for(int j = 0; j<array[0].length;j++){
						answer = answer+"Element ["+i+"]["+j+"="+ArrayFormulaEvaluatorHelper.evalToCellValue(array[i][j]).formatAsString()+"\n";
					}
				}
				return answer;
			}
			else {
				return ArrayFormulaEvaluatorHelper.evalToCellValue((ValueEval)value).formatAsString();
			}
		}
		else{
			if (value !=null) {
				return "Common object "+ value.toString();
			}
			return "NULL";
		}
	}


}
