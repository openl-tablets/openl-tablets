package com.exigen.poi.array.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestHelper {

	private Workbook wb;
	static Log log = LogFactory.getLog(TestHelper.class);

	
	public TestHelper(Workbook workBook){
		wb = workBook;
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
	
	
}
