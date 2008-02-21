/*
 * Created on Jun 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

/**
 * @author snshor
 *
 */
public class MeasurementSystem implements IMeasurementSystem, IBasicConstants
{
  String name;
  MassUnit baseMassUnit;
  DistanceUnit baseDistanceUnit;
  TimeUnit baseTimeUnit;

  MassUnit[] massUnits;
  DistanceUnit[] distanceUnits;
  TimeUnit[] timeUnits;
  
  
  
  public String printExpression(IMultiplicativeExpression im,  int doubleDigits)
  {
  
  	IDimension[] supportedDimensions = {Dimension.DISTANCE, Dimension.TIME, Dimension.MASS};
		IUnit[] baseUnits = {baseDistanceUnit, baseTimeUnit, baseMassUnit};
  	
  	
  	int dimCount = 0;
  	int negCount = 0;
  	int posCount = 0;
  	IDimensionPower[] powers = new IDimensionPower[supportedDimensions.length]; 

		IMultiplicativeExpression expr = new ScalarExpression(1);

  	for (int i = 0; i < supportedDimensions.length; i++)
    {
			IDimensionPower dp =  im.getDimensionPower(supportedDimensions[i]);
			if (dp == null)
			  continue;
			
			  
			  
			++dimCount;
			if (dp.getPower() < 0)
			{
				++negCount;
				for (int j = 0; j < Math.abs(dp.getPower()); ++j)
				{
					expr = expr.divide(baseUnits[i]);
				}
			}
			else
			{
				++posCount;
				for (int j = 0; j < dp.getPower(); ++j)
				{
					expr = expr.multiply(baseUnits[i]);
				}
			}
			powers[i] = dp;
    }
    
    
    
    if (dimCount == 0)
    {
    	return AMultiplicativeExpression.print(im,expr,"", doubleDigits);	
    }
    
    StringBuffer buf = new StringBuffer();

		if (posCount == 0)
		{
			buf.append("1");
		}
		else
		{
			boolean printed = false;
			for (int i = 0; i < powers.length; i++)
      {
        if (powers[i] == null)
          continue;
        int p = powers[i].getPower();
        if (p <= 0)
          continue;
        if (printed)
          buf.append('*');
				printed = true;        
        buf.append(baseUnits[i].getName());
        if (p > 1)
          buf.append("^" + p);  
          
      }
		}
		  
    
		if (negCount == 0)
		{
		}
		else
		{
			buf.append('/');
			boolean printed = false;
			for (int i = 0; i < powers.length; i++)
			{
				if (powers[i] == null)
					continue;
				int p = powers[i].getPower();
				if (p >= 0)
					continue;
				if (printed)
					buf.append('*');
				printed = true;        
				buf.append(baseUnits[i].getName());
				if (p > 1)
					buf.append("^" + (-p));  
          
			}
		}
		  
  	return AMultiplicativeExpression.print(im, expr,  buf.toString(), doubleDigits);
  }
  
  
  

  public MeasurementSystem(
    String name,
    MassUnit baseMassUnit,
    DistanceUnit baseDistanceUnit,
    TimeUnit baseTimeUnit,
    MassUnit[] massUnits,
    DistanceUnit[] distanceUnits,
    TimeUnit[] timeUnits)
  {
    this.name = name;
    this.baseMassUnit = baseMassUnit;
    this.baseDistanceUnit = baseDistanceUnit;
    this.baseTimeUnit = baseTimeUnit;
    this.massUnits = massUnits;
    this.timeUnits = timeUnits;
    this.distanceUnits = distanceUnits;
  }

  /**
   * @return
   */
  public DistanceUnit getBaseDistanceUnit()
  {
    return baseDistanceUnit;
  }

  /**
   * @return
   */
  public MassUnit getBaseMassUnit()
  {
    return baseMassUnit;
  }

  /**
   * @return
   */
  public TimeUnit getBaseTimeUnit()
  {
    return baseTimeUnit;
  }

  /**
   * @return
   */
  public DistanceUnit[] getDistanceUnits()
  {
    return distanceUnits;
  }

  /**
   * @return
   */
  public MassUnit[] getMassUnits()
  {
    return massUnits;
  }

  /**
   * @return
   */
  public String getName()
  {
    return name;
  }

  /**
   * @return
   */
  public TimeUnit[] getTimeUnits()
  {
    return timeUnits;
  }

  static class MetricSystem extends MeasurementSystem
  {

    /**
    * @param name
    * @param baseMassUnit
    * @param baseDistanceUnit
    * @param baseTimeUnit
    * @param massUnits
    * @param distanceUnits
    * @param timeUnits
    */
    public MetricSystem()
    {
      super(
        "metric",
         kg,
          m,
        s,
        new MassUnit[] {t, kg, g, mg },
        new DistanceUnit[] {
          km,
          m,
          cm,
          mm },
        new TimeUnit[] {
          week,
          day,
          h,
          min,
          s,
          ms,
          mks });
    }

  }
  
  static public final MeasurementSystem METRIC = new MetricSystem();
  
	public String getDisplayName(int mode)
	{
		return name;
	}
  
}
