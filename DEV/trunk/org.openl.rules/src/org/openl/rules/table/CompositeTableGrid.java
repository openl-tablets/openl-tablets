/**
 * Created May 17, 2009
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.table.AGridModel;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 * 
 * This is an implementation of a grid that is made of a set of IGridTables that could be stacked in 
 * horizontal or vertical order. The consequential application of the nested CompositeTableGrids would allow to 
 * create pretty complex grids and tables   
 * 
 */

public class CompositeTableGrid extends AGridModel {

	static class Transform {
		IGridTable gridTable;
		int tcol;
		int trow;

		public Transform(IGridTable table, int col, int row) {
			gridTable = table;
			this.tcol = col;
			this.trow = row;
		}
		
		public int col()
		{
			return gridTable.getGridColumn(tcol, trow);
		}
		
		public int row()
		{
			return gridTable.getGridRow(tcol, trow);
		}
		

		IGrid grid() {
			return gridTable.getGrid();
		}
	}

	IGridTable[] gridTables;
	ITransformer transformer;

	IGridRegion[] mappedRegions;

	IGridRegion[] mergedRegions;
	boolean vertical;

	int width = 0;

	int height = 0;

	public CompositeTableGrid(IGridTable[] tables, boolean vertical) {
		gridTables = tables;
		this.vertical = vertical;
		init();
	}
	
	public CompositeTableGrid(List<ILogicalTable> tables, boolean vertical)	{
		this.vertical = vertical;
		gridTables = new IGridTable[tables.size()];
		
		for (int i = 0; i < gridTables.length; i++) {
			gridTables[i] = tables.get(i).getGridTable();
		}
		
		init();
	}

	public IGridTable asGridTable() {
		return new GridTable(0, 0, height - 1, width - 1, this);
	}

	public ICell getCell(int column, int row) {
		Transform t = transform(column, row);
		if (t == null) {
			return null;
		}

		ICell delegate = t.grid().getCell(t.col(), t.row());

		return new CompositeCell(column, row, getGridRegionContaining(column, row), delegate);
	}

	public int getColumnWidth(int col) {
		Transform t = transform(col, 0);
		return t == null ? 100 : t.grid().getColumnWidth(t.col());
	}

	public int getHeight() {
		return height;
	}

	public int getMaxColumnIndex(int row) {
		return width - 1;
	}

	public int getMaxRowIndex() {
		return height - 1;
	}

	public IGridRegion getMergedRegion(int i) {
		return mergedRegions[i];
	}

	public int getMinColumnIndex(int row) {
		return 0;
	}

	public int getMinRowIndex() {
		return 0;
	}

	public int getNumberOfMergedRegions() {
		return mergedRegions.length;
	}

	public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
		Transform t1 = transform(colStart, rowStart);
		Transform t2 = transform(colEnd, rowEnd);
		if (t1 == null || t2 == null) {
			return null;
		}
		if (t1.grid() != t2.grid()) {
			return null;
		}
		return t1.grid().getRangeUri(t1.col(), t1.row(), t2.col(), t2.row());

	}

	public String getUri() {
		Transform t = transform(0, 0);
		return t == null ? null : t.grid().getUri();
	}

	public int getWidth() {
		return width;
	}

	void init() {
		boolean sameDim = true;
		int dim = -1;

		for (IGridTable gt : gridTables) {
			int h = gt.getLogicalHeight();
			int w = gt.getLogicalWidth();

			int dimX = vertical ? h : w;

			if (dim != dimX) {
				if (dim < 0)
					dim = dimX;
				else
					sameDim = false;
			}
			if (vertical) {
				height += h;
				width = Math.max(width, w);

			} else {
				width += w;
				height = Math.max(height, h);
			}

		}

		transformer = vertical ? (sameDim ? new VerticalSHTransformer()
				: new VerticalTransformer())
				: (sameDim ? new HorizontalSHTransformer()
						: new HorizontalTransformer());

		mappedRegions = new GridRegion[gridTables.length];

		int w = 0;
		int h = 0;

		for (int i = 0; i < gridTables.length; i++) {
			IGridRegion reg = gridTables[i].getRegion();
			GridRegion mapped = null;
			int last = i == gridTables.length - 1 ? 1 : 0;

			if (vertical) {
				int rh = IGridRegion.Tool.height(reg);
				mapped = new GridRegion(h, 0, h + rh - 1 + last, width);

				h += rh;
			} else {
				int rw = IGridRegion.Tool.width(reg);
				mapped = new GridRegion(0, w, height, w + rw - 1 + last);

				w += rw;

			}
			mappedRegions[i] = mapped;
		}

		HashSet<IGrid> gridSet = new HashSet<IGrid>();

		for (int i = 0; i < gridTables.length; i++) {
			gridSet.add(gridTables[i].getGrid());
		}

		ArrayList<IGridRegion> mergedRegionsList = new ArrayList<IGridRegion>();

		for (Iterator<IGrid> iter = gridSet.iterator(); iter.hasNext();) {
			IGrid grid = iter.next();

			int n = grid.getNumberOfMergedRegions();
			for (int i = 0; i < n; i++) {
				IGridRegion m = grid.getMergedRegion(i);

				for (int j = 0; j < gridTables.length; j++) {
					if (gridTables[j].getGrid() != grid) {
						continue;
					}
					IGridRegion reg = gridTables[j].getRegion();

					IGridRegion intersection = IGridRegion.Tool.intersect(m,
							reg);
					if (intersection != null) {
						int dx = mappedRegions[j].getLeft() - reg.getLeft();
						int dy = mappedRegions[j].getTop() - reg.getTop();
						IGridRegion moved = IGridRegion.Tool.move(intersection,
								dx, dy);
						mergedRegionsList.add(moved);
					}
				}
			}

		}

		mergedRegions = mergedRegionsList.toArray(new IGridRegion[0]);
	}

	public boolean isEmpty(int col, int row) {
		Transform t = transform(col, row);
		return t == null || t.grid().isEmpty(t.col(), t.row());
	}

	public Transform transform(int col, int row) {
		return transformer.transform(col, row);
	}

	static interface ITransformer {
		Transform transform(int col, int row);
	}

	class HorizontalTransformer implements ITransformer {

		public Transform transform(int col, int row) {
			Transform t = new Transform(null, col, row);
			return findHTable(t);
		}

		protected Transform findHTable(Transform t) {

			int startX = 0;
			for (IGridTable gt : gridTables) {
				int w = gt.getLogicalWidth();
				if (t.tcol < startX + w) {
					t.gridTable = gt;
					t.tcol -= startX;
					return t;
				}
				startX += w;
			}
			return null;

		}
	}

	class HorizontalSHTransformer extends HorizontalTransformer {
		protected Transform findHTable(Transform t) {
			if (t.tcol >= width)
				return null;
			int tw = width / gridTables.length;
			int idx = t.tcol / tw;
			t.gridTable = gridTables[idx];
			t.tcol %= tw;
			return t;
		}
	}

	class VerticalTransformer implements ITransformer {

		public Transform transform(int col, int row) {
			Transform t = new Transform(null, col, row);
			return findVTable(t);
		}

		protected Transform findVTable(Transform t) {

			int startY = 0;
			for (IGridTable gt : gridTables) {
				int h = gt.getLogicalHeight();
				if (t.trow < startY + h) {
					t.gridTable = gt;
					t.trow -= startY;
					return t;
				}
				startY += h;
			}
			return null;

		}
	}
	
	class VerticalSHTransformer extends VerticalTransformer {
		protected Transform findVTable(Transform t) {
			if (t.trow >= height)
				return null;
			int th = height / gridTables.length;
			int idx = t.trow / th;
			t.gridTable = gridTables[idx];
			t.trow %= th;
			return t;
		}
	}

}
