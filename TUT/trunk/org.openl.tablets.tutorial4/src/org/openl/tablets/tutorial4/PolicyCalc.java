package org.openl.tablets.tutorial4;

import org.openl.meta.DoubleValue;

public class PolicyCalc {

	Policy policy;
	String eligibility;

	DriverCalc[] driverCalcs;
	VehicleCalc[] vehicleCalcs;

	DoubleValue score = DoubleValue.ZERO;
	DoubleValue premium = DoubleValue.ZERO;

	public PolicyCalc(Policy policy) {
		this.policy = policy;

		init();
	}

	private void init() {

		this.driverCalcs = new DriverCalc[policy.drivers.length];
		this.vehicleCalcs = new VehicleCalc[policy.vehicles.length];

		for (int i = 0; i < policy.drivers.length; i++) {
			driverCalcs[i] = new DriverCalc(policy.drivers[i]);
		}

		for (int i = 0; i < policy.vehicles.length; i++) {
			vehicleCalcs[i] = new VehicleCalc(policy.vehicles[i]);
		}
	}

	public Policy getPolicy() {
		return policy;
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

	public DoubleValue getPremium() {
		return premium;
	}

	public void setPremium(DoubleValue premium) {
		this.premium = premium;
	}

	public DriverCalc[] getDriverCalcs() {
		return driverCalcs;
	}

	public VehicleCalc[] getVehicleCalcs() {
		return vehicleCalcs;
	}

}
