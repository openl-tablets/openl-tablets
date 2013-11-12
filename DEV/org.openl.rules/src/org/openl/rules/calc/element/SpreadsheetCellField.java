package org.openl.rules.calc.element;

import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField {

	protected SpreadsheetCell cell;

	public static SpreadsheetCellField createSpreadsheetCellField(
			IOpenClass declaringClass, String name, SpreadsheetCell cell) {
		if (cell.getKind() == SpreadsheetCellType.METHOD)
			return new SpreadsheetCellField(declaringClass, name, cell);
		return new ConstSpreadsheetCellField(declaringClass, name, cell);
	}

	SpreadsheetCellField(IOpenClass declaringClass, String name,
			SpreadsheetCell cell) {
		super(declaringClass, name, cell.getType());

		this.cell = cell;
	}

	@Override
	public Object get(Object target, IRuntimeEnv env) {

		if (cell.getKind() == SpreadsheetCellType.METHOD) {
			SpreadsheetResultCalculator result = (SpreadsheetResultCalculator) target;

			return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
		}

		return cell.getValue();

	}

	public SpreadsheetCell getCell() {
		return cell;
	}

	@Override
	public IOpenClass getType() {
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

		ConstSpreadsheetCellField(IOpenClass declaringClass, String name,
				SpreadsheetCell cell) {
			super(declaringClass, name, cell);
			this.value = cell.getValue();
		}

		@Override
		public Object get(Object target, IRuntimeEnv env) {
			if (cell.getKind() == SpreadsheetCellType.METHOD)
				return super.get(target, env);

			return cell.getValue();
		}

	}

}
