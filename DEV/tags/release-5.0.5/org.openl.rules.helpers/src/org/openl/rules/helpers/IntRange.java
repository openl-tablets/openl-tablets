/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.domain.IntRangeDomain;
import org.openl.util.StringTool;

/**
 * @author snshor
 */
public class IntRange extends IntRangeDomain implements INumberRange
{
    
    
    class Parser
    {
	final int CLOSE = 0, OPEN = 1;
	int lowerBound = Integer.MIN_VALUE;

	int upperBound = Integer.MAX_VALUE;

	int lowerType = CLOSE;

	int upperType = CLOSE;
	
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
		
		min = lowerBound + lowerType;
		max = upperBound - upperType;
	}

    }
    
    

	public IntRange(String s)
	{
	    super(0,0);
	    new Parser().parse(s);
	}



	
	
	

//	public String toString()
//	{
//		if (lowerBound == upperBound)
//			return String.valueOf(lowerBound);
//		if (lowerBound == Integer.MIN_VALUE)
//			return "<" + upperBound;
//		if (upperBound == Integer.MAX_VALUE)
//			return "" + lowerBound + "+";
//
//		return "" + lowerBound + "-" + upperBound;
//	}

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
