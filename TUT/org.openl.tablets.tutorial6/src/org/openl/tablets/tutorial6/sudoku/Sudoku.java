package org.openl.tablets.tutorial6.sudoku;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalImpl;
import com.exigen.ie.constrainer.GoalPrintSolutionNumber;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.IntVarSelectorFirstUnbound;
import com.exigen.ie.constrainer.impl.ConstraintAllDiff;
import com.exigen.ie.constrainer.impl.IntVarImpl;

public class Sudoku
{

    // it is 3 for classic SUDOKU
    int SIZE;

    final int SQ_SIZE()
    {
	return SIZE * SIZE;
    }

    final int TOTAL_SIZE()
    {
	return SQ_SIZE() * SQ_SIZE();
    }

    int[][] initialValues;

    Constrainer c = new Constrainer("Sudoku");

    public Sudoku(int size, int[][] values)
    {

	SIZE = size;
	if (values.length != SQ_SIZE())
	    throw new RuntimeException("The array size should be "
		    + TOTAL_SIZE());
	initialValues = values;
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
		if (initialValues[i][j] > 0)
		{
		    c.postConstraint(values[i][j].equals(initialValues[i][j]));
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

	    IntExpArray ary = makeSquare(square);

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

    class SaveResult extends GoalImpl
    {
	IntExpArray ary;

	int[][] result;

	public SaveResult(Constrainer c, IntExpArray ary)
	{
	    super(c);
	    this.ary = ary;
	}

	public Goal execute() throws Failure
	{

	    result = new int[SQ_SIZE()][SQ_SIZE()];
	    for (int i = 0; i < SQ_SIZE(); i++)
	    {
		for (int j = 0; j < SQ_SIZE(); j++)
		{
		    result[i][j] = ary.get(i * SQ_SIZE() + j).value();
		}
	    }
	    return null;
	}

	/**
	 * @return Returns the result.
	 */
	public int[][] getResult()
	{
	    return result;
	}

    }

    class PrintSquare extends GoalImpl
    {
	IntExpArray ary;

	public PrintSquare(Constrainer c, IntExpArray ary)
	{
	    super(c);
	    this.ary = ary;
	}

	public Goal execute() throws Failure
	{
	    System.out.println("=========================");
	    for (int i = 0; i < SQ_SIZE(); i++)
	    {
		for (int j = 0; j < SQ_SIZE(); j++)
		{
		    System.out.print(" " + ary.get(i * SQ_SIZE() + j).value());
		}
		System.out.println();
	    }
	    return null;
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

    IntExpArray makeSquare(int sq)
    {
	IntExpArray ary = new IntExpArray(c, SQ_SIZE());
	for (int i = 0; i < SQ_SIZE(); i++)
	{
	    int row = sq / SIZE * SIZE + i / SIZE;

	    int col = sq % SIZE * SIZE + i % SIZE;

	    ary.set(values[row][col], i);
	}
	return ary;
    }

    void solveAndPrint() throws Failure
    {
	initialize();

	IntExpArray allValues = allValues();

	Goal search_goal = new GoalGenerate(allValues);
	Goal print_goal = new PrintSquare(c, allValues);

	Goal goal = new GoalAnd(search_goal, new GoalPrintSolutionNumber(c),
		print_goal);
	c.execute(goal);

    }
    
    void solveAndPrint2() throws Failure
    {
	initialize();

	IntExpArray allValues = allValues();

//	Goal search_goal = new GoalGenerate(allValues, new IntVarSelectorMinSize(allValues), false);
	Goal search_goal = new GoalGenerate(allValues, new IntVarSelectorFirstUnboundRandom(allValues), false);
	Goal print_goal = new PrintSquare(c, allValues);

	Goal goal = new GoalAnd(search_goal, new GoalPrintSolutionNumber(c),
		print_goal);
	c.execute(goal);

    }
    
    
    

    public int[][] solve() throws Failure
    {
	initialize();

	IntExpArray allValues = allValues();

	Goal search_goal = new GoalGenerate(allValues);
	SaveResult save_goal = new SaveResult(c, allValues);

	Goal goal = new GoalAnd(search_goal, save_goal);
	c.execute(goal);

	return save_goal.getResult();

    }
    
    public int[][] solve2() throws Failure
    {
	initialize();

	IntExpArray allValues = allValues();

//	Goal search_goal = new GoalGenerate(allValues, new IntVarSelectorMinSize(allValues), false);
	Goal search_goal = new GoalGenerate(allValues, new PrintingSelector(allValues), false);
	SaveResult save_goal = new SaveResult(c, allValues);

	Goal goal = new GoalAnd(search_goal, save_goal);
	c.execute(goal);

	return save_goal.getResult();

    }
    
    
    static class PrintingSelector extends IntVarSelectorFirstUnbound
    {

	long n;
	public PrintingSelector(IntExpArray intvars)
	{
	    super(intvars);
	}

	@Override
	public int select()
	{
	    int x = super.select();
	    ++n;
	    max = Math.max(x, max);
	    if (n % 1000 == 0)
		System.out.println(max + " : " + n+ " : " + x);
	    return x;
	}
	
	int max = -1;
	
	
	
    };


    
        

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

    public static void main(String[] args) throws Failure
    {

	System.out.println("===== Data 1 =======");
	solveOnce(3,data1);
	solveOnce(3,data1);
	solveOnce2(3,data1);
	// solveOnce(data1);
	// solveOnce(data1);
	// solveOnce(data1);

	System.out.println("===== Data 2 =======");
	solveOnce(3,data2);
	solveOnce2(3,data2);
	// solveOnce(data2);
	// solveOnce(data2);
	// solveOnce(data2);

	System.out.println("===== Data 3 =======");
	solveOnce(3,data3);
	solveOnce2(3,data3);

	System.out.println("===== Data 4 =======");
	solveOnce2(4,data4);
//	solveOnce(4,data4);
	
	
	System.out.println("===== END  =======");
	// solveOnce(data);
	// solveOnce(data);
	// solveOnce(data);
	// solveOnce(data);

    }

    static void solveOnce(int n,int[][] x) throws Failure
    {
	
	long start = System.currentTimeMillis();
	new Sudoku(n, x).solveAndPrint();
	long end = System.currentTimeMillis();
	System.out.println("Time to solve: " + (end - start) + "ms");

    }
    
    
    static void solveOnce2(int n,int[][] x) throws Failure
    {
	
	long start = System.currentTimeMillis();
	new Sudoku(n, x).solveAndPrint2();
	long end = System.currentTimeMillis();
	System.out.println("Time to solve 2: " + (end - start) + "ms");

    }    

    static int[][] data1 = { 
	    { 0, 5, 6,/**/8, 0, 1,/**/9, 4, 0, },
	    { 9, 0, 0,/**/6, 0, 5,/**/0, 0, 3, },
	    { 7, 0, 0,/**/4, 9, 3,/**/0, 0, 8, },
	    /** ***** ***** ***** */
	    { 8, 9, 7,/**/0, 4, 0,/**/6, 3, 5, },
	    { 0, 0, 3,/**/9, 0, 6,/**/8, 0, 0, },
	    { 4, 6, 5,/**/0, 8, 0,/**/2, 9, 1, },
	    /** ***** ***** ***** */
	    { 5, 0, 0,/**/2, 6, 9,/**/0, 0, 7, },
	    { 6, 0, 0,/**/5, 0, 4,/**/0, 0, 9, },
	    { 0, 4, 9,/**/7, 0, 8,/**/3, 5, 0, }, 
	    };
    
    
    static int[][] data3 = {     
    {0, 2, 0, 0, 0, 0,  0, 0, 0,}, 
    {0, 0, 0, 6, 0, 0,  0, 0, 3,}, 
    {0, 7, 4, 0, 8, 0,  0, 0, 0,}, 
    {0, 0, 0, 0, 0, 3,  0, 0, 2,}, 
    {0, 8, 0, 0, 4, 0,  0, 1, 0,}, 
    {6, 0, 0, 5, 0, 0,  0, 0, 0,}, 
    {0, 0, 0, 0, 1, 0,  7, 8, 0,}, 
    {5, 0, 0, 0, 0, 9,  0, 0, 0,}, 
    {0, 0, 0, 0, 0, 0,  0, 4, 0,}, 
};
    
    
   static int[][] data4 = { 
	    
	    { 0, 10,  0,  0, 16,  0,  0, 14,  0,  0,  0,  0, 15, 13,  0,  0, }, 
	    { 0, 11,  6,  0,  2,  0,  1,  0,  0,  4,  0,  0,  0,  9,  0,  0, },
	    { 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 12,  0,  0,  0,  7, },
	    {13,  0,  0,  0,  0, 10,  0,  0,  0,  0, 15,  0,  5,  3,  8,  0, },
	    { 0,  0,  0,  8,  6,  0,  0, 11,  4,  3,  0, 14,  0,  0,  0,  0, },
	    { 0, 15,  0,  0,  0,  0,  8,  2, 13,  0,  0,  0, 14,  1,  4,  0, },
	    { 0,  0, 16,  7,  0,  5,  0,  0,  0,  1,  0,  0,  0,  2,  0, 13, },  
	    { 0, 14,  0,  0, 12,  0,  0,  0, 16,  9, 10,  0,  0,  0,  0,  0, },
	    { 0,  0,  0,  0,  0, 13,  7,  3,  0,  0,  0,  4,  0,  0, 11,  0, },
	    { 5,  0, 12,  0,  0,  0, 15,  0,  0,  0,  8,  0, 10, 14,  0,  0, },
	    { 0,  2,  9, 14,  0,  0,  0, 10,  3, 16,  0,  0,  0,  0, 12,  0, },
	    { 0,  0,  0,  0, 14,  0,  4,  5,  1,  0,  0,  6, 16,  0,  0,  0, },
	    { 0, 16, 13,  9,  0,  8,  0,  0,  0,  0, 14,  0,  0,  0,  0,  6, },
	    {12,  0,  0,  0, 10,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, },
	    { 0,  0,  4,  0,  0,  0,  9,  0,  0, 11,  0,  7,  0, 15,  3,  0, },
	    { 0,  0,  1,  3,  0,  0,  0,  0,  5,  0,  0,  2,  0,  0,  7,  0, },
	    };
    

    static int[][] data2 = { { 0, 8, 0,/**/0, 0, 6,/**/0, 0, 5, },
	    { 2, 0, 0,/**/0, 0, 0,/**/4, 8, 0, },
	    { 0, 0, 9,/**/0, 0, 8,/**/0, 1, 0, },
	    /** ***** ***** ***** */
	    { 0, 0, 0,/**/0, 8, 0,/**/1, 0, 2, },
	    { 0, 0, 0,/**/3, 0, 1,/**/0, 0, 0, },
	    { 6, 0, 1,/**/0, 9, 0,/**/0, 0, 0, },
	    /** ***** ***** ***** */
	    { 0, 9, 0,/**/4, 0, 0,/**/8, 0, 0, },
	    { 0, 7, 6,/**/0, 0, 0,/**/0, 0, 3, },
	    { 1, 0, 0,/**/7, 0, 0,/**/0, 5, 0, }, };

    static int[][] data = { { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    /** ***** ***** ***** */
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    /** ***** ***** ***** */
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
	    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, }, };
}