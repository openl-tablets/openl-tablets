package sudoku;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalPrintSolutionNumber;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ConstraintAllDiff;
import org.openl.ie.constrainer.impl.IntVarImpl;

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
			throw new RuntimeException("The array size should be " + TOTAL_SIZE());
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

			c.postConstraint(new ConstraintAllDiff(ary));
		}

		// square constraints
		for (int square = 0; square < SQ_SIZE(); square++)
		{

			IntExpArray ary = makeSquare(square);

			c.postConstraint(new ConstraintAllDiff(ary));
		}

		// square constraints
		for (int row = 0; row < SQ_SIZE(); row++)
		{

			IntExpArray ary = makeRow(row);

			c.postConstraint(new ConstraintAllDiff(ary));
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

	void solve() throws Failure
	{
		initialize();

		IntExpArray allValues = allValues();

		Goal search_goal = new GoalGenerate(allValues);
		Goal print_goal = new PrintSquare(c, allValues);

		Goal goal = new GoalAnd(search_goal, new GoalPrintSolutionNumber(c),
				print_goal);
		c.execute(goal);

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

	public static void main(String[] args) throws Failure
	{

System.out.println("===== Data 1 =======");
	    	solveOnce(data1);
		solveOnce(data1);
		solveOnce(data1);
		solveOnce(data1);

		System.out.println("===== Data 2 =======");
		solveOnce(data2);
		solveOnce(data2);
		solveOnce(data2);
		solveOnce(data2);

		
		System.out.println("===== Data  =======");
		solveOnce(data);
		solveOnce(data);
		solveOnce(data);
		solveOnce(data);
		
	}

	static void solveOnce(int[][] x) throws Failure
	{
		long start = System.currentTimeMillis();
		new Sudoku(3, x).solve();
		long end = System.currentTimeMillis();
		System.out.println("Time to solve: " + (end - start) + "ms");

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
    { 0, 4, 9,/**/7, 0, 8,/**/3, 5, 0, }, };
  
  static int[][] data2 = { 
    { 0, 8, 0,/**/0, 0, 6,/**/0, 0, 5, },
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

  
  static int[][] data = { 
    { 0, 0, 0,/**/0, 0, 0,/**/0, 0, 0, },
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