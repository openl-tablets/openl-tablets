package org.apache.poi.hssf.record.formula.functions;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Class contains information for udf function evaluation: input and output
 * cells.
 * 
 * @author SPetrakovsky
 * 
 */
public abstract class UserDefinedFunction implements FreeRefFunction {

	private List<Cell> inputCells;
	private Cell outputCell;

	public List<Cell> getInputCells() {
		return inputCells;
	}

	public void setInputCells(List<Cell> inputCells) {
		this.inputCells = inputCells;
	}

	public Cell getOutputCell() {
		return outputCell;
	}

	public void setOutputCell(Cell outputCell) {
		this.outputCell = outputCell;
	}

}
