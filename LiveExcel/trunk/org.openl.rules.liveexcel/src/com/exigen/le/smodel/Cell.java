package com.exigen.le.smodel;

import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * LiveExcel Cell
 * @author vabramovs
 *
 */
public class Cell implements ExcelSpace {
	private static final Log LOG = LogFactory.getLog(Cell.class);
	
// Excel visual addressing:  [excel]<sheet>!A1
private String sheetName="";
private String workbookName="";
private String column="Undefined";   
private int columnIndex=(-1);   
private int row;

private final static  char beg = 'A';
private final static char end = 'Z';
private final static int mult = end-beg+1;


/**
 * Cell by "human" representative "A1"
 * @param cAddress
 * @return
 */
public Cell init(String cAddress){
	String cellAddress = cAddress;
	 try {
		 if(cellAddress.contains("[")){
			 workbookName = cellAddress.substring(cellAddress.indexOf("[")+1, cellAddress.indexOf("]")).trim();
			 cellAddress = cellAddress.substring(cellAddress.indexOf("]")+1);
		 }
		 cellAddress = cellAddress+" "; //strange but otherwise split does  nor work correctly
		 String[]part = cellAddress.split("[0-9]");
		 this.column = part[0];
		 this.row = Integer.parseInt(cellAddress.trim().substring(column.length()));
		 int pos = column.indexOf("!");
		 if(pos>0){
			 this.sheetName = this.column.substring(0,pos).trim();
			 this.column = this.column.substring(pos+1).trim();
		 }
	} catch (NumberFormatException e) {
		LOG.error("Could not parse cell adress:"+cellAddress,e);
		return null;
	}   
	return this;
} 
public boolean isArea() {
	return false;
}

public Cell from() {
	return this;
}

public Cell to() {
	return this;
}

public String toString() {
	String result ="";
	if(workbookName.length()>0){
		result = "["+workbookName+"]";
	}
	if(sheetName.length()>0){
		result = result+sheetName+"!";
	}
		return result + column + row;
}

/**
 * @return the workbookName
 */
public String getWorkbookName() {
	return workbookName;
}
/**
 * @param workbookName the workbookName to set
 */
public void setWorkbookName(String workbookName) {
	this.workbookName = workbookName;
}
public String getColumn() {
	return column;
}

public int getColumnIndex() {
	if(columnIndex == (-1)){
		this.columnIndex=IndexCalculator.calculateIndex(column.toUpperCase().toCharArray())-1;
	}
	return columnIndex;
}
public void setColumn(String column) {
	this.column = column;
}
public void setColumnIndex(int columnIndex){
	this.columnIndex = columnIndex;
	// TODO - write convert from ColumnIndex to column
	column = convertColumnToString(columnIndex+1);
}

public int getRow() {
	return row;
}
public int getRowIndex() {
	return row-1;
}

public void setRow(int row) {
	this.row = row;
}

public void setSheetName(String sheetName){
	this.sheetName=sheetName;
}
public String getSheetName(){
	return sheetName;
}
public int getHeight() {
	return 1;
}
public int getWidth() {
	return 1;
}
private static String convertColumnToString(int colIndex){
  return 	new String(IndexCalculator.calculateColumn(colIndex));
}
}
