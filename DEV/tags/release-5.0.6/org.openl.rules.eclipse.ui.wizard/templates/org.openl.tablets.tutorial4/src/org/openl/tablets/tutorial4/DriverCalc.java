/**
 * Created Mar 15, 2007
 */
package org.openl.tablets.tutorial4;

import org.openl.meta.DoubleValue;

/**
 * @author snshor
 *
 */
public class DriverCalc
{

	
	String ageType;
	String eligibility;
	String driverRisk;
	
	DoubleValue score = new DoubleValue(0);

	Driver driver;
	
	DoubleValue premium = DoubleValue.ZERO;
	
	public DriverCalc(Driver driver)
	{
		this.driver = driver;
	}
	
	public DriverCalc()
	{
	}

	
	public String getAgeType()
	{
		return this.ageType;
	}

	public void setAgeType(String ageType)
	{
		this.ageType = ageType;
	}

	public String getDriverRisk()
	{
		return this.driverRisk;
	}

	public void setDriverRisk(String driverRisk)
	{
		this.driverRisk = driverRisk;
	}

	public String getEligibility()
	{
		return this.eligibility;
	}

	public void setEligibility(String eligibility)
	{
		this.eligibility = eligibility;
	}

	public DoubleValue getScore()
	{
		return this.score;
	}

	public void setScore(DoubleValue score)
	{
		this.score = score;
	}
	
	public DoubleValue getFinalScore()
	{
		return score.copy("Score for " + driver.getName());
	}


	public Driver getDriver()
	{
		return this.driver;
	}


	public void setDriver(Driver driver)
	{
		this.driver = driver;
	}

	public DoubleValue getPremium()
	{
		return this.premium;
	}

	public void setPremium(DoubleValue premium)
	{
		this.premium = premium;
	}
	

}
