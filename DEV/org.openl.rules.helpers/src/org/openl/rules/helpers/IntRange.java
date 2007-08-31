/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.util.StringTool;

/**
 * @author snshor
 */
public class IntRange implements INumberRange
{
	static int CLOSE = 0, OPEN = 1;

	public IntRange(String s)
	{
		parse(s);
	}

	int lowerBound = Integer.MIN_VALUE;

	int upperBound = Integer.MAX_VALUE;

	int lowerType = CLOSE;

	int upperType = CLOSE;

	public boolean contains(int x)
	{
		return lowerBound + lowerType <= x && x <= upperBound - upperType;
	}

	public boolean containsNumber(Number num)
	{
		return lowerBound + lowerType <= num.doubleValue() && num.doubleValue() <= upperBound - upperType;
	}

	
	public int getMin()
	{
		return lowerBound + lowerType;
	}

	public int getMax()
	{
		return upperBound - upperType;
	}
	
	
	void parse(String s)
	{
		String[] numbers = StringTool.tokenize(s, "-+< ");

		switch (numbers.length)
		{
		case 0:
			throw new RuntimeException("Range Format Error - no numbers");
		case 1:
		{
			int n = (int)parseNumber(numbers[0]);
			if (s.indexOf('<') >= 0)
			{
				upperBound = n;
				upperType = OPEN;
			} else if (s.indexOf('+') >= 0)
			{
				lowerBound = n;
			} else
			{
				upperBound = lowerBound = n;
			}
		}
			break;
		case 2:
		{
			lowerBound = (int)parseNumber(numbers[0]);
			upperBound = (int)parseNumber(numbers[1]);
			if (s.indexOf('-') >= 0)
			{
			} else
				throw new RuntimeException(
						"Range Format Error - allowed format: N1 - N2");
		}
			break;
		default:
			throw new RuntimeException(
					"Range Format Error - no more than two numbers allowed");

		}
	}

	/**
	 * @return Returns the cLOSE.
	 */
	public static int getCLOSE()
	{
		return CLOSE;
	}

	/**
	 * @param close
	 *          The cLOSE to set.
	 */
	public static void setCLOSE(int close)
	{
		CLOSE = close;
	}

	/**
	 * @return Returns the oPEN.
	 */
	public static int getOPEN()
	{
		return OPEN;
	}

	/**
	 * @param open
	 *          The oPEN to set.
	 */
	public static void setOPEN(int open)
	{
		OPEN = open;
	}

	/**
	 * @return Returns the lowerBound.
	 */
	public int getLowerBound()
	{
		return lowerBound;
	}

	/**
	 * @param lowerBound
	 *          The lowerBound to set.
	 */
	public void setLowerBound(int lowerBound)
	{
		this.lowerBound = lowerBound;
	}

	/**
	 * @return Returns the lowerType.
	 */
	public int getLowerType()
	{
		return lowerType;
	}

	/**
	 * @param lowerType
	 *          The lowerType to set.
	 */
	public void setLowerType(int lowerType)
	{
		this.lowerType = lowerType;
	}

	/**
	 * @return Returns the upperBound.
	 */
	public int getUpperBound()
	{
		return upperBound;
	}

	/**
	 * @param upperBound
	 *          The upperBound to set.
	 */
	public void setUpperBound(int upperBound)
	{
		this.upperBound = upperBound;
	}

	/**
	 * @return Returns the upperType.
	 */
	public int getUpperType()
	{
		return upperType;
	}

	/**
	 * @param upperType
	 *          The upperType to set.
	 */
	public void setUpperType(int upperType)
	{
		this.upperType = upperType;
	}

	public String toString()
	{
		if (lowerBound == upperBound)
			return String.valueOf(lowerBound);
		if (lowerBound == Integer.MIN_VALUE)
			return "<" + upperBound;
		if (upperBound == Integer.MAX_VALUE)
			return "" + lowerBound + "+";

		return "" + lowerBound + "-" + upperBound;
	}

	static public double parseNumber(String s)
	{
		if (s.startsWith("$"))
			s = s.substring(1);
		char c = s.charAt(s.length() - 1);
		double multiplier = 1;
		switch (c)
		{
		case 'M':
			multiplier = 1000000;
			break;
		case 'K':
			multiplier = 1000;
			break;
		case 'B':
			multiplier = 1000000000;
			break;
		}
		
		
		if (multiplier != 1)
			s = s.substring(0,s.length()-1);

		return Double.parseDouble(s) * multiplier;
		
	}


}
