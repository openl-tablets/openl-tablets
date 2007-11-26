package org.openl.tablets.tutorial6.sudoku;

import org.openl.tablets.tutorial6.sudoku.Sudoku.SaveResult;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalSaveArrayResult;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.impl.ConstraintAllDiff;
import com.exigen.ie.constrainer.impl.IntVarImpl;

public class SudokuSolver
{
    
    
    int h_size, v_size;
    int[][] data;
    
    Constrainer c= new Constrainer("Sudoku");
    
    
    // it is 3 for classic SUDOKU
  //  int SIZE;

    public SudokuSolver(int h_size, int v_size, int[][] data)
    {
	this.h_size = h_size;
	this.v_size = v_size;
	this.data = data;
    }

    final int SQ_SIZE()
    {
	return h_size * v_size;
    }

    final int TOTAL_SIZE()
    {
	return SQ_SIZE() * SQ_SIZE();
    }
    
    
    static public int[][] solve(int h_size, int v_size, int[][] data) throws Failure
    {
	return new SudokuSolver(h_size, v_size, data).solve();
	
    }
    
    int[][] solve() throws Failure{
	
	initialize();

	IntExpArray allValues = allValues();

	Goal search_goal = new GoalGenerate(allValues);
	GoalSaveArrayResult save_goal = new GoalSaveArrayResult(c, allValues);

	Goal goal = new GoalAnd(search_goal, save_goal);
	c.execute(goal);

	return  convert2d(save_goal.getFirstResult());

    }
    
    private int[][] convert2d(int[] firstResult)
    {
	int sq_size = SQ_SIZE();
	int[][] res = new int[sq_size][sq_size];
	for (int i = 0; i < sq_size; i++)
	{
	    for (int j = 0; j < sq_size; j++)
	    {
		res[i][j] = firstResult[i*sq_size + j];
		
	    }
	}
	
	return res;
    }

    private IntExpArray allValues()
    {
	IntExpArray ary = new IntExpArray(c, TOTAL_SIZE());
	for (int i = 0; i < SQ_SIZE(); i++)
	{
	    for (int j = 0; j < SQ_SIZE(); j++)
	    {
		ary.set(values[i][j], i * SQ_SIZE() + j);
	    }

	}

	return ary;
    }
    
    
    IntVar[][] values; 
    
    void initialize() throws Failure
    {
	values = new IntVar[SQ_SIZE()][SQ_SIZE()];

	for (int i = 0; i < SQ_SIZE(); i++)
	{
	    for (int j = 0; j < SQ_SIZE(); j++)
	    {
		values[i][j] = new IntVarImpl(c, 1, SQ_SIZE());
		if (data[i][j] > 0)
		{
		    c.postConstraint(values[i][j].equals(data[i][j]));
		}
	    }
	}

	// add column constraints

	for (int column = 0; column < SQ_SIZE(); column++)
	{

	    IntExpArray ary = makeColumn(column);

	    try
	    {
		c.postConstraint(new ConstraintAllDiff(ary));
	    } catch (Failure f)
	    {
		throw new RuntimeException("Duplicated Values in Column "
			+ (column + 1));
	    }
	}

	// square constraints
	for (int square = 0; square < SQ_SIZE(); square++)
	{

	    IntExpArray ary = makeRect(square);

	    try
	    {
		c.postConstraint(new ConstraintAllDiff(ary));

	    } catch (Failure f)
	    {
		throw new RuntimeException("Duplicated Values in Square "
			+ (square + 1));
	    }
	}

	// square constraints
	for (int row = 0; row < SQ_SIZE(); row++)
	{

	    IntExpArray ary = makeRow(row);

	    try
	    {
		c.postConstraint(new ConstraintAllDiff(ary));
	    } catch (Failure f)
	    {
		throw new RuntimeException("Duplicated Values in Row "
			+ (row + 1));
	    }
	}

    }   
    IntExpArray makeColumn(int col)
    {
	IntExpArray ary = new IntExpArray(c, SQ_SIZE());
	for (int i = 0; i < SQ_SIZE(); i++)
	{
	    ary.set(values[i][col], i);
	}
	return ary;
    }    
    
    IntExpArray makeRow(int row)
    {
	IntExpArray ary = new IntExpArray(c, SQ_SIZE());
	for (int i = 0; i < SQ_SIZE(); i++)
	{
	    ary.set(values[row][i], i);
	}
	return ary;
    }
    
  /**
   * There is as many rectangles in Sudoku as cells in the rectangle itself
   * @param rect
   * @return
   */
    
    IntExpArray makeRect(int rect)
    {
	IntExpArray ary = new IntExpArray(c, SQ_SIZE());
	
	int rect_row_size = v_size;
	
	
	
	int startRow = rect/rect_row_size * v_size;
	
	int startCol = rect % rect_row_size * h_size;
	
	
	
	for (int row =0 ; row < v_size; row++)
	{
	    for (int col = 0; col < h_size; ++col)
	    {	
		ary.set(values[startRow + row][startCol + col], row * h_size + col);
	    } 
	}
	return ary;
    }
    
    
}
