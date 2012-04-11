package org.openl.tablets.tutorial6.sudoku;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalSaveArrayResult;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.impl.ConstraintAllDiff;
import com.exigen.ie.constrainer.impl.IntVarImpl;


/**
 * 
 * @author snshor
 *
 * SudokuSolver solves different kinds of the Sudoku-family problems by applying straightforward
 * constraints that exactly match Sudoku rules - i.e no duplicates in any row, columns or small rectangle. 
 * 
 * In addition to the traditional 3x3(or you may say it is 9x9 large square) Sudoku, the
 * Solver is capable of solving other sizes like 3x4, 3x5, 4x4 etc.   
 *
 */

public class SudokuSolver {

	int 
		H, // height of the small rectangle(square)   
		W; // width of the small rectangle(square)
	int[][] data;

	Constrainer c = new Constrainer("Sudoku");
	IAreaResolver area;
	
	public SudokuSolver(int H, int W, int[][] data, IAreaResolver res) 
	{
		this.H = H;
		this.W = W;
		this.data = data;
		this.area = res;
	}
	
	static interface IAreaResolver
	{
		IntExp find(int nArea, IntExp[][] matrix, int nExp);
	}
	

	// it is 3 x 3 for classic SUDOKU

	public SudokuSolver(int H, int W, int[][] data) 
	{
		this(H, W, data, null);
	}

	
	
	
	
	/**
	 * 
	 * @return the number of cells in the single rectangle, the number coincides 
	 * with the large square side length
	 */
	final int RECT_SIZE() {
		return H * W;
	}

	final int SIDE() {
		return RECT_SIZE();
	}
	
	final int TOTAL_SQUARE_SIZE() {
		return RECT_SIZE() * RECT_SIZE();
	}

	static public int[][] solve(int H, int W, int[][] data)
			throws Failure {
		return new SudokuSolver(H, W, data).solve();

	}

	public int[][] solve() throws Failure {

		initialize();

		IntExpArray allCells = populateAllCells();

		Goal search_goal = new GoalGenerate(allCells);
		GoalSaveArrayResult save_goal = new GoalSaveArrayResult(c, allCells);

		Goal goal = new GoalAnd(search_goal, save_goal);
		c.execute(goal);

		return convert2d(save_goal.getFirstResult());

	}

	/**
	 * 
	 * Converts one-dimensional result into 2d matrix
	 * 
	 * @param resultAry
	 * @return
	 */
	
	private int[][] convert2d(int[] resultAry) {
		int side = SIDE();
		int[][] res = new int[side][side];
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				res[i][j] = resultAry[i * side + j];

			}
		}

		return res;
	}
	
	
	

	
    /**
     * 
     * @return all the values in array. Converts IntVar[][] matrix into  IntExpArray
     */	
	private IntExpArray populateAllCells() {
		IntExpArray ary = new IntExpArray(c, TOTAL_SQUARE_SIZE());
		for (int i = 0; i < SIDE(); i++) {
			for (int j = 0; j < SIDE(); j++) {
				ary.set(values[i][j], i * SIDE() + j);
			}

		}

		return ary;
	}

	IntVar[][] values;

	void initialize() throws Failure {
		
		//create matrix
		values = new IntVar[SIDE()][SIDE()];

		//First we post equality constraints for all non-zero values in the data matrix
		
		for (int i = 0; i < SIDE(); i++) {
			for (int j = 0; j < SIDE(); j++) {
				values[i][j] = new IntVarImpl(c, 1, SIDE());
				if (data[i][j] > 0) {
					c.postConstraint(values[i][j].equals(data[i][j]));
				}
			}
		}

		
		// Now we apply ConstraintAllDiff to all the columns, rows and rectangles  
		
		
		// add columns constraints
		

		for (int column = 0; column < SIDE(); column++) {

			IntExpArray ary = makeColumn(column);

			try {
				c.postConstraint(new ConstraintAllDiff(ary));
			} catch (Failure f) {
				throw new RuntimeException("Duplicated Values in Column "
						+ (column + 1));
			}
		}


		// row constraints
		for (int row = 0; row < SIDE(); row++) {

			IntExpArray ary = makeRow(row);

			try {
				c.postConstraint(new ConstraintAllDiff(ary));
			} catch (Failure f) {
				throw new RuntimeException("Duplicated Values in Row "
						+ (row + 1));
			}
		}

		
		//
		// square(rectangle) constraints
		
		if (area == null)
			area = new RectAreaResolver();
		
		for (int rect = 0; rect < RECT_SIZE(); rect++) {

			IntExpArray ary = new IntExpArray(c, SIDE());
//			IntExpArray ary = makeRect(rect);
			
			for (int k = 0; k < ary.size(); k++) {
				ary.set(area.find(rect, values, k), k);
			}

			try {
				c.postConstraint(new ConstraintAllDiff(ary));

			} catch (Failure f) {
				throw new RuntimeException("Duplicated Values in Rectangle(Area) "
						+ (rect + 1));
			}
		}
	}
	
	

	private IntExpArray makeColumn(int col) {
		IntExpArray ary = new IntExpArray(c, SIDE());
		for (int i = 0; i < SIDE(); i++) {
			ary.set(values[i][col], i);
		}
		return ary;
	}

	private IntExpArray makeRow(int row) {
		IntExpArray ary = new IntExpArray(c, SIDE());
		for (int i = 0; i < SIDE(); i++) {
			ary.set(values[row][i], i);
		}
		return ary;
	}

	/**
	 * There are as many rectangles in Sudoku large square as there are cells in the rectangle itself;
	 * 
	 * For example, if rectangle  HxW == 5x3 there are 5 rectangles in the row and 3 rows 
	 * of rectangles - total 15  
	 * 
	 * 
	 * @param rect
	 * @return
	 */

	IntExpArray makeRect(int rect) {
		IntExpArray ary = new IntExpArray(c, SIDE());

		int rect_row_size = H;

		int startRow = rect / rect_row_size * H;

		int startCol = rect % rect_row_size * W;

		for (int row = 0; row < H; row++) {
			for (int col = 0; col < W; ++col) {
				ary.set(values[startRow + row][startCol + col], row * W
						+ col);
			}
		}
		return ary;
	}

	
	class RectAreaResolver implements IAreaResolver
	{

		public IntExp find(int area, IntExp[][] matrix, int exp) {
			int rect_row_size = H;

			int startRow = area / rect_row_size * H;

			int startCol = area % rect_row_size * W;
			int row = exp / W;
			int col = exp % W;

			return matrix[startRow + row][startCol + col];
		}
		
	}
	
}
