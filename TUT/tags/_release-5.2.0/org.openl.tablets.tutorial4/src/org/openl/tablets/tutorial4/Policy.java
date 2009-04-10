/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import java.util.Vector;

import org.openl.base.NamedThing;
import org.openl.meta.DoubleValue;
import org.openl.meta.DoubleValueFunction;

/**
 * @author snshor
 *
 */
public class Policy extends NamedThing 
{

	Driver[] drivers;
	Vehicle[] vehicles;
	
	String clientTier;
	String clientTerm;
	
	
	/*** Rating attributes *******
	 * To simplify demo logic we keep rating attributes together with basic
	 * BOM. This approach is not recommended in real-life implementation 
	 * 
	 * */	
		
	
//	DoubleValue score = new DoubleValue();
	
	
	Vector scores = new Vector();
	
	public void addScore(DoubleValue score)
	{
		scores.add(score);
	}
	
	
	
  String eligibility; 
	

	/**
	 * @return Returns the drivers.
	 */
	public Driver[] getDrivers() {
		return drivers;
	}

	/**
	 * @param drivers The drivers to set.
	 */
	public void setDrivers(Driver[] drivers) {
		this.drivers = drivers;
	}

	
	public DoubleValue getFinalScore()
	{
		DoubleValue[] scs = (DoubleValue[])scores.toArray(new DoubleValue[0]);
		
		double sum = 0;
		for (int i = 0; i < scs.length; i++)
		{
			sum += scs[i].doubleValue();
		}
		
		DoubleValue dv = new DoubleValueFunction(sum, "SUM", scs);
		
		return new NamedDoubleValue("Score for " + getName(), dv).getResult();
	}
	
	


	/**
	 * @return Returns the vehicles.
	 */
	public Vehicle[] getVehicles() {
		return vehicles;
	}

	/**
	 * @param vehicles The vehicles to set.
	 */
	public void setVehicles(Vehicle[] vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * @return Returns the clientTier.
	 */
	public String getClientTier() {
		return clientTier;
	}

	/**
	 * @param clientTier The clientTier to set.
	 */
	public void setClientTier(String clientTier) {
		this.clientTier = clientTier;
	}

	/**
	 * @return Returns the clientTerm.
	 */
	public String getClientTerm() {
		return clientTerm;
	}

	/**
	 * @param clientTerm The clientTerm to set.
	 */
	public void setClientTerm(String clientTerm) {
		this.clientTerm = clientTerm;
	}

	/**
	 * @return Returns the eligibility.
	 */
	public String getEligibility() {
		return eligibility;
	}

	/**
	 * @param eligibility The eligibility to set.
	 */
	public void setEligibility(String eligibility) {
		this.eligibility = eligibility;
	}
	
}
