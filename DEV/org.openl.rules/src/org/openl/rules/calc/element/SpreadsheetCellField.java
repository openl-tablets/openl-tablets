package org.openl.rules.calc.element;

import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField {

	protected SpreadsheetCell cell;
	private SpreadsheetStructureBuilder spreadsheetBuilder;

	public static SpreadsheetCellField createSpreadsheetCellField(SpreadsheetStructureBuilder structureBuilder, IOpenMethodHeader spreadsheetHeader,
			IOpenClass declaringClass, String name, SpreadsheetCell cell) {
		if (cell.getKind() == SpreadsheetCellType.METHOD)
			return new SpreadsheetCellField(structureBuilder, spreadsheetHeader, declaringClass, name, cell);
		return new ConstSpreadsheetCellField(structureBuilder, spreadsheetHeader, declaringClass, name, cell);
	}

	SpreadsheetCellField(SpreadsheetStructureBuilder spreadsheetBuilder, IOpenMethodHeader spreadsheetHeader, IOpenClass declaringClass, String name,
			SpreadsheetCell cell) {
		super(declaringClass, name, cell.getType());

		this.cell = cell;
		this.spreadsheetBuilder = spreadsheetBuilder;
	}

	@Override
	public Object get(Object target, IRuntimeEnv env) {

//		if (cell.getKind() == SpreadsheetCellType.METHOD) {
			SpreadsheetResultCalculator result = (SpreadsheetResultCalculator) target;

			return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
//		}
//
//		return cell.getValue();

	}

	public SpreadsheetCell getCell() {
		return cell;
	}

	@Override
	public IOpenClass getType() {
		
		IOpenClass type = cell.getType();
		if (type == null)
		{
			type = spreadsheetBuilder.makeType(cell);
			if (type == null)
			{
				spreadsheetBuilder.makeType(cell);
				throw new RuntimeException("Type cannot be defined");

			}	
			spreadsheetBuilder = null;
			
		}	
		
		return cell.getType();
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public void set(Object target, Object value, IRuntimeEnv env) {
		throw new UnsupportedOperationException(
				"Can not write to spreadsheet cell result");
	}

	public Point getRelativeCoordinates() {
		return new Point(getCell().getColumnIndex(), getCell().getRowIndex());
	}

	public Point getAbsoluteCoordinates() {
		return new Point(getCell().getSourceCell().getAbsoluteColumn(),
				getCell().getSourceCell().getAbsoluteRow());
	}

	static class ConstSpreadsheetCellField extends SpreadsheetCellField {

		Object value;

		ConstSpreadsheetCellField(SpreadsheetStructureBuilder structureBuilder, IOpenMethodHeader spreadsheetHeader, IOpenClass declaringClass, String name,
				SpreadsheetCell cell) {
			super(structureBuilder, spreadsheetHeader, declaringClass, name, cell);
			this.value = cell.getValue();
		}

		@Override
		public Object get(Object target, IRuntimeEnv env) {
//			if (cell.getKind() == SpreadsheetCellType.METHOD)
//				return super.get(target, env);

			return cell.getValue();
		}

	}

}
