package org.openl.tablets.tutorial6.sudoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.ASelector;
import org.openl.util.ISelector;

import org.openl.ie.constrainer.IntExp;

public class SquigglySudoku extends SudokuSolver {
	public SquigglySudoku(String methodName, int H, int W, int[][] data) {
		super(H, W, data, makeAreaResolver(methodName));
		// TODO Auto-generated constructor stub
	}

	private static IAreaResolver makeAreaResolver(String methodName) {
		SudokuWrapper sw = new SudokuWrapper();
		IGridTable gt = GameInterface.findTable("sq1", sw.getInstance());
		gt = new GridTable(gt.getRegion(), gt.getGrid());

		List<List<XY>> matrix = SquigglySudoku.selectByColor(gt);
		
		return new SquigglyAreaResolver(matrix);
		
		
	}
	
	
	static class SquigglyAreaResolver implements IAreaResolver
	{

		List<List<XY>> list;
		public SquigglyAreaResolver(List<List<XY>> matrix) {
			this.list = matrix;
		}

		public IntExp find(int area, IntExp[][] matrix, int exp) {
			XY point = list.get(area).get(exp);
			
			System.out.println("A"+area+ ":" + exp + ":" + matrix[point.y-1][point.x] );
			
			return matrix[point.y-1][point.x];
		}
		
	}

	static class XY {

		public String toString() {
			return "(" + x + "," + y + ")";
		}

		public boolean equals(Object obj) {
			return x == ((XY) obj).x && y == ((XY) obj).y;
		}

		public int hashCode() {
			return x * 37 + y;
		}

		int x, y;

		public XY(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}

		XY left() {
			return new XY(x - 1, y);
		}

		XY right() {
			return new XY(x + 1, y);
		}

		XY top() {
			return new XY(x, y - 1);
		}

		XY bottom() {
			return new XY(x, y + 1);
		}

	}

	static void select(XY point, Set<XY> taken, List<XY> selected, ISelector<XY> sel) {
		System.out.println("Selecting: " + point);

		if (!sel.select(point))
			return;
		if (taken.contains(point))
			return;

		System.out.println("Yes");
		taken.add(point);
		selected.add(point);

		select(point.left(), taken, selected, sel);
		select(point.right(), taken, selected, sel);
		select(point.top(), taken, selected, sel);
		select(point.bottom(), taken, selected, sel);
	}

	static class RegionColorSelector extends ASelector<XY> {

		IGridTable table;
		short[] color;
		int w, h;

		boolean first = true;

		public RegionColorSelector(IGridTable table) {
			super();
			this.table = table;
			this.w = table.getGridWidth();
			this.h = table.getGridHeight();
		}

		static boolean sameColor(short[] c1, short[] c2) {
			if (c1 == c2)
				return true;
			if (c1 == null || c2 == null)
				return false;
			for (int i = 0; i < c2.length; i++) {
				if (c1[i] != c2[i])
					return false;
			}
			return true;
		}

		static short[] getColor(IGridTable gt, XY point) {
			ICellStyle cs = gt.getCell(point.x, point.y).getStyle();
			return cs == null ? null : cs.getFillForegroundColor();
		}

		public boolean select(XY point) {

			if (first) {
				System.out.println("First:" + point);
				first = false;
				color = getColor(table, point);
				return true;
			}

			if (point.x < 0 || point.y < 1 || point.x >= w || point.y >= h)
				return false;
			return sameColor(color, getColor(table, point));
		}
	}

	static List<List<XY>> selectByColor(IGridTable table) {
		List<List<XY>> res = new ArrayList<List<XY>>();

		Set<XY> taken = new HashSet<XY>();

		while (true) {
			XY nextPoint = findNextPoint(table, taken);

			if (nextPoint == null)
				return res;

			System.out.println("Next:" + nextPoint
					+ table.getCell(nextPoint.x, nextPoint.y).getStringValue());

			List<XY> selected = new ArrayList<XY>();

			RegionColorSelector sel = new RegionColorSelector(table);
			select(nextPoint, taken, selected, sel);
			res.add(selected);
		}

	}

	static private XY findNextPoint(IGridTable table, Set<XY> taken) {
		IGridRegion g = table.getRegion();
		int w = IGridRegion.Tool.width(g);
		int h = IGridRegion.Tool.height(g);
		for (int x = 0; x < w; x++) {
			for (int y = 1; y < h; y++) {
				XY point = new XY(x, y);
				if (!taken.contains(point))
					return point;

			}
		}

		return null;
	}

	public static void main(String[] args) {
		SudokuWrapper sw = new SudokuWrapper();
		IGridTable gt = GameInterface.findTable("sq1", sw.getInstance());
		gt = new GridTable(gt.getRegion(), gt.getGrid());

		List<List<XY>> res = SquigglySudoku.selectByColor(gt);

		for (Iterator<List<XY>> iterator = res.iterator(); iterator.hasNext();) {
			List<XY> list = iterator.next();
			System.out.println(list);
		}
	}

}
