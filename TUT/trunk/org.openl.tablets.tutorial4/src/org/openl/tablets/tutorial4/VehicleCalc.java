/**
 * Created Jan 31, 2007
 */
package org.openl.tablets.tutorial4;

import org.openl.meta.DoubleValue;

/**
 * @author snshor
 * 
 */
public class VehicleCalc {

	Vehicle vehicle;
	String theftRating;
	String injuryRating;
	String eligibility;

	DoubleValue score = DoubleValue.ZERO;
	DoubleValue premium = DoubleValue.ZERO;

	public VehicleCalc(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public String getTheftRating() {
		return theftRating;
	}

	public void setTheftRating(String theftRating) {
		this.theftRating = theftRating;
	}

	public String getInjuryRating() {
		return injuryRating;
	}

	public void setInjuryRating(String injuryRating) {
		this.injuryRating = injuryRating;
	}

	public String getEligibility() {
		return eligibility;
	}

	public void setEligibility(String eligibility) {
		this.eligibility = eligibility;
	}

	public DoubleValue getScore() {
		return score;
	}

	public void setScore(DoubleValue score) {
		this.score = score;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public DoubleValue getPremium() {
		return premium;
	}

	public void setPremium(DoubleValue premium) {
		this.premium = premium;
	}

}
