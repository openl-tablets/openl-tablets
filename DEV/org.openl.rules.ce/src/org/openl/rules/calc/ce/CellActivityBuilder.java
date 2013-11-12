package org.openl.rules.calc.ce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetRangeField;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.util.ce.IActivity;

public class CellActivityBuilder {

	SpreadsheetCell[][] cells;

	public CellActivityBuilder(SpreadsheetCell[][] cells) {
		super();
		this.cells = cells;
	}

	public IActivity[] buildActivities() {

		List<CellActivity> list = new ArrayList<CellActivity>(cells.length * 3);
		Map<SpreadsheetCell, CellActivity> map = new HashMap<SpreadsheetCell, CellActivity>(
				list.size());
		List<List<SpreadsheetCell>> depList = new ArrayList<List<SpreadsheetCell>>();

		for (int i = 0; i < cells.length; i++) {
			SpreadsheetCell[] row = cells[i];
			for (int j = 0; j < row.length; j++) {
				SpreadsheetCell cell = row[j];
				IOpenMethod cellMethod = cell.getMethod();
				if (cellMethod == null)
					continue;

				List<SpreadsheetCell> deps = findDependencies(cellMethod);
				CellActivity act = new CellActivity(cell);
				map.put(cell, act);
				list.add(act);
				depList.add(deps);
			}

		}

		for (int i = 0; i < list.size(); i++) {
			CellActivity act = list.get(i);

			List<SpreadsheetCell> deps = depList.get(i);

			List<IActivity> dependsOn = new ArrayList<IActivity>();
			for (SpreadsheetCell cell : deps) {
				IActivity dep = map.get(cell);
				dependsOn.add(dep);
			}
			act.setDependsOn(dependsOn);
		}

		return list.toArray(new IActivity[list.size()]);
	}

	private List<SpreadsheetCell> findDependencies(IOpenMethod cellMethod) {

		List<SpreadsheetCell> list = new ArrayList<SpreadsheetCell>();

		BindingDependencies dep = ((CompositeMethod)cellMethod).getDependencies();

		Collection<IOpenField> fields = dep.getFieldsMap().values();

		for (IOpenField field : fields) {

			if (field instanceof SpreadsheetCellField) {
				SpreadsheetCellField sf = (SpreadsheetCellField) field;
				if (sf.getCell().isMethodCell())
					add(list, sf.getCell());
				continue;
			}

			if (field instanceof SpreadsheetRangeField) {
				SpreadsheetRangeField range = (SpreadsheetRangeField) field;
				extractRangeCells(range, list);
				continue;
			}

		}

		return list;
	}

	private void extractRangeCells(SpreadsheetRangeField range,
			List<SpreadsheetCell> list) {
		SpreadsheetCellField start = range.getStart();
		SpreadsheetCellField end = range.getEnd();

		int sx = start.getCell().getColumnIndex();
		int sy = start.getCell().getRowIndex();
		int ex = end.getCell().getColumnIndex();
		int ey = end.getCell().getRowIndex();

		int w = ex - sx + 1;
		int h = ey - sy + 1;


		for (int x = 0; x < w; ++x)
			for (int y = 0; y < h; ++y) {

				SpreadsheetCell sc = cells[sy + y][sx + x];
				if (sc.isMethodCell())
					add(list, sc);
			}

	}
	
	private void add(List<SpreadsheetCell> list, SpreadsheetCell sc)
	{
		if (!list.contains(sc))
			list.add(sc);
	}
	

}
