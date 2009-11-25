/**
 * 
 */
package com.exigen.liveexcel.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;



/**
 * @author vabramovs
 *
 */
public class TaskPrescription {
	
	protected String functionName;
	protected Object[] args;
	protected Object result;
	protected int iterationCount;
	
	public TaskPrescription(String functionName,Object[] args,Object result,int iterationCount ){
		this.functionName = functionName;
		this.args = args;
		this.result = result;
		 this.iterationCount = iterationCount;
		
	}

	/**
	 * @return the functionName
	 */
	public String getFunctionName() {
		return functionName;
	}

	/**
	 * @return the args
	 */
	public Object[] getArgs() {
		return args;
	}

	/**
	 * @return the result
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * @return the iterationCount
	 */
	public int getIterationCount() {
		return iterationCount;
	}

	static public List<TaskPrescription> getTasks4Sheet(Sheet sheet){
		List<TaskPrescription> answer = new  ArrayList<TaskPrescription>();
		Iterator<Row> rowIt = sheet.iterator();
		while(rowIt.hasNext()){
			Row row = rowIt.next();
			Vector<Object> rowPar = new Vector<Object>(); 
			Iterator<Cell> cellIt = row.cellIterator();
			
			while(cellIt.hasNext()){
				Cell cell = cellIt.next();
				int type = cell.getCellType();
				switch (type)
				{
					case Cell.CELL_TYPE_BLANK:
					case Cell.CELL_TYPE_FORMULA:
					case Cell.CELL_TYPE_ERROR:
						break;
					case Cell.CELL_TYPE_BOOLEAN:
						rowPar.add(new Boolean(cell.getBooleanCellValue()));
						break;
					case Cell.CELL_TYPE_NUMERIC:
						rowPar.add(new Double(cell.getNumericCellValue()));
						break;
					case Cell.CELL_TYPE_STRING:
						rowPar.add(cell.getStringCellValue());
						break;
				}
			}
			if(rowPar.size() >=3){
				String funcName = (String)rowPar.get(0);
				if(!funcName.substring(0,1).matches("[_a-zA-Z]")) // skip all wrong function name (allow to comment task)
					continue;
					
				int iteration = ((Double)rowPar.get(1)).intValue();
				Object result = rowPar.get(2); 
				rowPar.removeElementAt(0);
				rowPar.removeElementAt(0);
				rowPar.removeElementAt(0);
	//			rowPar.removeElementAt(rowPar.size()-1);
	//          rowPar.removeElementAt(rowPar.size()-1);
			
				answer.add(new TaskPrescription(funcName,rowPar.toArray(),result,iteration ));
			
			}
			
		}
		
		return answer;
		
	} 
}
