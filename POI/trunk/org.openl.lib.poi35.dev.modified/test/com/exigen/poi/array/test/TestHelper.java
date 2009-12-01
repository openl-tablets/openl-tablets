package com.exigen.poi.array.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Ignore;

@Ignore
public class TestHelper {

	private Workbook wb;
	static Log log = LogFactory.getLog(TestHelper.class);

	
	public TestHelper(Workbook workBook){
		wb = workBook;
	}
	
	public TestHelper(){
		
	}
	
	public void readWorkbook(String fileName){
		
		int fileType =0;
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex>0){
			if (fileName.substring(dotIndex+1).equalsIgnoreCase("xls")){
				fileType=1;
			} else if (fileName.substring(dotIndex+1).equalsIgnoreCase("xlsx")){
				fileType=2;
			}
			
		}
		if (fileType ==0 ){
			log.error("file type not supported.: " + fileName );
			throw new IllegalArgumentException("file type not supported.: " + fileName);
		}
		
		
		URL url = TestHelper.class.getClassLoader().getResource(fileName);
		if (url == null){
			log.equals("failed to find: " + fileName);
			throw new IllegalArgumentException("failed to find: " + fileName);
		}

		try {
			if (fileType == 1){
				wb =  new HSSFWorkbook(new FileInputStream(url.getFile()));
			} else {
				wb = new XSSFWorkbook(url.getFile());
			}
		} catch (IOException ioe){
			log.error("Failed to open test workbook from file:" + url.getFile(), ioe );
			throw new IllegalArgumentException(url.getFile());
		}
		
	}
	
	
	
	
	protected Cell getCell(String cellRef){
		
		log.debug("Access to Cell:" + cellRef);
		Sheet sheet = wb.getSheetAt(0);
		CellReference cellReference = new CellReference(cellRef); 
		Row row = sheet.getRow(cellReference.getRow());
		if (row==null){
			throw new IllegalArgumentException("Illegal access to cell:" + cellRef);
		}
		Cell cell = row.getCell(cellReference.getCol()); 
		if (cell==null){
			throw new IllegalArgumentException("Illegal access to cell:" + cellRef);
		}
		return cell;
		
	}
	
	protected double calculateNumericFormula(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_NUMERIC){
			log.error("not numeric result: " + type);
			throw new IllegalArgumentException("Not numeric type" + type);
		}
		
		double result = cell.getNumericCellValue();
		log.debug("Calculated:" + result);
		return result;
	}
	
	protected String calculateStringFormula(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_STRING){
			log.error("not string result: " + type);
			throw new IllegalArgumentException("Not string type" + type);
		}
		
		String result = cell.getStringCellValue();
		log.debug("Calculated:" + result);
		return result;
	}
	
	protected Boolean calculateBooleanFormula(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_BOOLEAN) {
			log.error("not boolean result: " + type);
			throw new IllegalArgumentException("Not boolean type" + type);
		}
		
		Boolean result = cell.getBooleanCellValue();
		log.debug("Calculated:" + result);
		return result;
	}

	
	
	
	protected FormulaError calculateNumericFormulaWithError(Cell cell){
		
		if (cell.getCellType() != Cell.CELL_TYPE_FORMULA){
			log.error("Not formula in cell: " + cell.toString());
			throw new IllegalArgumentException("Not formula" + cell.toString());
		}

		log.debug("Formula: " + cell.getCellFormula());
		FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
		int type = evaluator.evaluateFormulaCell(cell);
		if (type != Cell.CELL_TYPE_ERROR){
			log.error("not error result");
			throw new IllegalArgumentException("Not error type: " + type);
		}
		
		byte result = cell.getErrorCellValue();
		log.debug("Error value:" + result);
		return FormulaError.forInt(result);
		
	}
	
	protected FormulaError calculateNumericFormulaWithError(String cellRef){
		return calculateNumericFormulaWithError(getCell(cellRef));
	}		
	
	protected String calculateStringFormula(String cellRef){
		
		return calculateStringFormula(getCell(cellRef));
	}	
	protected double calculateNumericFormula(String cellRef){
		
		return calculateNumericFormula(getCell(cellRef));
	}	

	protected boolean calculateBooleanFormula(String cellRef){
		
		return calculateBooleanFormula(getCell(cellRef));
	}	
	
	
	protected double getNumericValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_NUMERIC){
			log.error("Not numeric in cell:" + cell.toString());
			throw new IllegalArgumentException("Not numeric:" + cell.toString());
		}
		double result = cell.getNumericCellValue();
		log.debug("value: " + result );
		return result;
	}

	protected String getStringValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_STRING){
			log.error("Not string in cell:" + cell.toString());
			throw new IllegalArgumentException("Not string:" + cell.toString());
		}
		String result = cell.getStringCellValue();
		log.debug("value: " + result );
		return result;
	}
	
	protected Boolean getBooleanValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_BOOLEAN){
			log.error("Not boolean in cell:" + cell.toString());
			throw new IllegalArgumentException("Not boolean:" + cell.toString());
		}
		Boolean result = cell.getBooleanCellValue();
		log.debug("value: " + result );
		return result;
	}
	
	protected void setNumericValue(Cell cell, double value){
		cell.setCellValue(value);
	}
	
	
	protected void setNumericValue(String cellRef, double value){
		setNumericValue(getCell(cellRef), value);
	}
	
	protected double getNumericValue(String cellRef){
		
		return getNumericValue(getCell(cellRef));
	}

	protected String getStringValue(String cellRef){
		
		return getStringValue(getCell(cellRef));
	}

	protected Boolean getBooleanValue(String cellRef){
		
		return getBooleanValue(getCell(cellRef));
	}
	
	
	
	public boolean calculateAndCompareNumericArray(char calculateFromColumn, int calculateFromRow, char calculateToColumn,
			int calculateToRow, char compareFromColumn, int compareFromRow, double precision){
		
		if (calculateFromColumn> calculateToColumn)
			throw new IllegalArgumentException("columns: " + calculateFromColumn + "   " + calculateToColumn);
		
		if (calculateFromRow > calculateToRow)
			throw new IllegalArgumentException("rows: " + calculateFromRow + "   " + calculateToRow);
		
		for(int c=0; calculateFromColumn + c <= calculateToColumn; c++ ){
			for(int r=0; calculateFromRow +r <= calculateToRow; r++){
				char ch[] = new char[1];
				ch[0] = (char)(calculateFromColumn+c);
				String calculateRef = (new String(ch)) + (calculateFromRow+r);
				double calcResult = calculateNumericFormula(calculateRef);
				
				ch[0]=(char)(compareFromColumn+c);
				String compareRef = (new String(ch)) + (compareFromRow + r);
				double compareResult = getNumericValue(compareRef);
				
				if (Math.abs(compareResult - calcResult) > precision){
					// comparison failed
					log.debug("Array comparison failed. " + "Calculated cell:" + calculateRef + " Compared cell: " + compareRef +
					"   calculated result: " + calcResult + "  compared result: " + compareResult);		
					return false;
				}
				
			}
		}
		return true;
	}
	protected void setArrayFormula(String cellRef, String formula, String range){
	    Sheet sheet = getCell(cellRef).getSheet();
	    sheet.setArrayFormula(formula, CellRangeAddress.valueOf(range));
	}
    protected void removeArrayFormula(String cellRef){
    	Cell cell = getCell(cellRef);
        Sheet sheet = cell.getSheet();
        sheet.removeArrayFormula(cell);
    }

	protected String getErrorValue(Cell cell){
		if (cell.getCellType() != Cell.CELL_TYPE_ERROR){
			log.error("Not numeric in cell:" + cell.toString());
			throw new IllegalArgumentException("Not error:" + cell.toString());
		}
		byte result = cell.getErrorCellValue();
		log.debug("value: " + result );
		return org.apache.poi.ss.usermodel.ErrorConstants.getText(result);
	}

	protected String getErrorValue(String cellRef){
		
		return getErrorValue(getCell(cellRef));
	}
	protected  int getCellType(String cellRef) {
		 Cell cell = getCell(cellRef);
		return cell.getCellType(); 
	}

}
