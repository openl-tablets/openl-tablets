/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import org.openl.util.StringTool;

/**
 * @author snshor
 */
public class DoubleRange implements INumberRange
{
    static double CLOSE = 0, OPEN = 0.0000000001;

    public DoubleRange(String s)
    {
        parse(s);
    }
    
    public DoubleRange(double lowerBound, double upperBound)
    {
    	this.lowerBound = lowerBound;
    	this.upperBound = upperBound;
    }

    double lowerBound = Double.MIN_VALUE;

    double upperBound = Double.MAX_VALUE;

    double lowerType = CLOSE;

    double upperType = CLOSE;

    public boolean contains(double x)
    {
        return lowerBound + lowerType <= x && x <= upperBound - upperType;
    }

  	public boolean containsNumber(Number num)
  	{
  		return lowerBound + lowerType <= num.doubleValue() && num.doubleValue() <= upperBound - upperType;
  	}

    
    
    void parse(String s)
    {
        String[] numbers = StringTool.tokenize(s, "-+<= ");

        switch (numbers.length)
        {
        case 0:
            throw new RuntimeException("Range Format - 0");
        case 1:
        {
            double n = IntRange.parseNumber(numbers[0]);
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
            lowerBound = IntRange.parseNumber(numbers[0]);
            upperBound = IntRange.parseNumber(numbers[1]);
            if (s.indexOf('-') >= 0)
            {
            } else
                throw new RuntimeException("Range Format - 2");
        }
            break;
        default:
            throw new RuntimeException("Range Format - 3");

        }
    }
    /**
     * @return Returns the cLOSE.
     */
    public static double getCLOSE()
    {
        return CLOSE;
    }
    /**
     * @param close The cLOSE to set.
     */
    public static void setCLOSE(double close)
    {
        CLOSE = close;
    }
    /**
     * @return Returns the oPEN.
     */
    public static double getOPEN()
    {
        return OPEN;
    }
    /**
     * @param open The oPEN to set.
     */
    public static void setOPEN(double open)
    {
        OPEN = open;
    }
    /**
     * @return Returns the lowerBound.
     */
    public double getLowerBound()
    {
        return lowerBound;
    }
    /**
     * @param lowerBound The lowerBound to set.
     */
    public void setLowerBound(double lowerBound)
    {
        this.lowerBound = lowerBound;
    }
    /**
     * @return Returns the lowerType.
     */
    public double getLowerType()
    {
        return lowerType;
    }
    /**
     * @param lowerType The lowerType to set.
     */
    public void setLowerType(double lowerType)
    {
        this.lowerType = lowerType;
    }
    /**
     * @return Returns the upperBound.
     */
    public double getUpperBound()
    {
        return upperBound;
    }
    /**
     * @param upperBound The upperBound to set.
     */
    public void setUpperBound(double upperBound)
    {
        this.upperBound = upperBound;
    }
    /**
     * @return Returns the upperType.
     */
    public double getUpperType()
    {
        return upperType;
    }
    /**
     * @param upperType The upperType to set.
     */
    public void setUpperType(double upperType)
    {
        this.upperType = upperType;
    }

		public boolean contains(DoubleRange range)
		{
			return lowerBound <= range.lowerBound && range.upperBound <= upperBound;
		}

		public DoubleRange intersect(DoubleRange range)
		{
			double l = Math.max(lowerBound, range.lowerBound);
			double u = Math.min(upperBound, range.upperBound);
			return l > u ? NaRange : new DoubleRange(l,u); 
		}
		
		static public final DoubleRange NaRange = new DoubleRange(1,0);
}
