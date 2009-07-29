/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import org.openl.meta.DoubleValue;
import org.openl.vm.Tracer;

/**
 * @author snshor
 * 
 */
public class Tutorial4Main {

	/**
	 * Tutorial wrapper object instance.
	 */
	private Tutorial_4Wrapper wrapper = new Tutorial_4Wrapper();

	public static void main(String[] args) {

		Tracer.setTracer(new Tracer());
		new Tutorial4Main().execute();
	}

	public void execute() {

		useCase1Example();
		useCase2Example();
		useCase3Example();
		useCase4Example();
	}

	/**
	 * Preferred Client Business Rule V1. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase1Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile1();
		Policy policy1 = policies[0];

		calculatePolicyPremium(policy1);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase2Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile2();
		Policy policy1 = policies[0];

		calculatePolicyPremium(policy1);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase3Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile3();
		Policy policy1 = policies[0];

		calculatePolicyPremium(policy1);
	}

	/**
	 * Eligibility Within and Outside an Elite Client Relationship. See
	 * '2005_Product_Derby.pdf' document for more details about tutorial use
	 * case.
	 */
	private void useCase4Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile4();
		Policy policy1 = policies[0];

		calculatePolicyPremium(policy1);
	}

	private void calculatePolicyPremium(Policy policy) {

		System.out.println("*");
		System.out.println("* Calculate premium amount for policy '"
				+ policy.getName() + "'");
		System.out.println("*");

		// Step 1.
		// Calculate auto eligibility for each vehicle in policy.
		//
		System.out.println("\n1) Auto eligibility:");

		// Get vehicles.
		//
		Vehicle[] vehicles1 = policy.getVehicles();

		VehicleCalc[] vehicleWrappers1 = new VehicleCalc[vehicles1.length];

		// One by one calculate eligibility.
		//
		for (int i = 0; i < vehicles1.length; i++) {

			Vehicle vehicle = vehicles1[i];
			VehicleCalc vehicleWrapper = new VehicleCalc(vehicle);

			calculateAutoEligibility(vehicleWrapper);

			vehicleWrappers1[i] = vehicleWrapper;

			// Print out the result.
			//
			System.out.println();
			printAutoEligibility(vehicleWrapper);
		}

		// Step 2.
		// Calculate driver eligibility for each driver person in policy.
		//
		System.out.println("\n2) Driver eligibility and risk:");

		// Get drivers.
		//
		Driver[] drivers1 = policy.getDrivers();

		// Initialize array of wrappers for drivers objects.
		// Each driver wrapper object stores calculation results.
		// 
		DriverCalc[] driverWrappers1 = new DriverCalc[drivers1.length];

		// One by one calculate eligibility.
		//
		for (int i = 0; i < drivers1.length; i++) {

			Driver driver = drivers1[i];
			DriverCalc driverWrapper = new DriverCalc(driver);

			calculateDriverEligibility(driverWrapper);

			driverWrappers1[i] = driverWrapper;

			// Print out the result.
			//
			System.out.println();
			printDriverEligibility(driverWrapper);
		}

		// Step 3.
		// Calculate policy scores.
		//
		System.out.println("\n3) Policy eligibility:");

		calculatePolicyEligibility(policy, vehicleWrappers1, driverWrappers1);

		// Print out the result.
		//
		System.out.println();
		printPolicyEligibility(policy);

		// Step 4.
		// Calculate auto premium.
		//
		System.out.println("\n4) Vehicle premium (with discounts):");

		for (int i = 0; i < vehicleWrappers1.length; i++) {

			VehicleCalc vehicleWrapper = vehicleWrappers1[i];

			calculateAutoPremium(vehicleWrapper);

			// Print out the result.
			//
			System.out.println();
			printAutoPremium(vehicleWrapper);
		}

		// Step 5.
		// Calculate drivers premium.
		//
		System.out.println("\n5) Driver premium:");

		for (int i = 0; i < driverWrappers1.length; i++) {

			DriverCalc driverWrapper = driverWrappers1[i];

			calculateDriverPremium(driverWrapper);

			// Print out the result.
			//
			System.out.println();
			printDriverPremium(driverWrapper);
		}

		// Step 6.
		// Calculate policy premium.
		//
		System.out.println("---------------------------------------");

		DoubleValue policyPremium = DoubleValue.ZERO;

		for (int i = 0; i < driverWrappers1.length; i++) {

			DriverCalc driverWrapper = driverWrappers1[i];
			policyPremium = DoubleValue.add(policyPremium, driverWrapper
					.getPremium());
		}

		for (int i = 0; i < vehicleWrappers1.length; i++) {

			VehicleCalc vehicleWrapper = vehicleWrappers1[i];
			policyPremium = DoubleValue.add(policyPremium, vehicleWrapper
					.getPrice());
		}

		DoubleValue clientTierDiscount = wrapper.clientDiscount(policy
				.getClientTier());
		policyPremium = DoubleValue.subtract(policyPremium, clientTierDiscount);

		System.out.println("  Policy premium: " + policyPremium);
		System.out.println();
	}

	private void calculateAutoEligibility(VehicleCalc vehicleWrapper) {

		Vehicle vehicle = vehicleWrapper.getVehicle();

		String theftRating = wrapper.theftRating(vehicle);
		vehicleWrapper.setTheftRating(theftRating);

		String injuryRating = wrapper.injuryRating(vehicle);
		vehicleWrapper.setInjuryRating(injuryRating);

		String eligibility = wrapper.vehicleEligibility(vehicleWrapper);
		vehicleWrapper.setEligibility(eligibility);
	}

	private void printAutoEligibility(VehicleCalc vehicleWrapper) {

		System.out.println("Vehicle: " + vehicleWrapper.getVehicle().getName());
		System.out
				.println("	Theft rating:  " + vehicleWrapper.getTheftRating());
		System.out.println("	Injury rating: "
				+ vehicleWrapper.getInjuryRating());
		System.out
				.println("	Eligibility:   " + vehicleWrapper.getEligibility());
	}

	private void calculateDriverEligibility(DriverCalc driverWrapper) {

		Driver driver = driverWrapper.getDriver();

		String ageType = wrapper.driverAgeType(driver);
		driverWrapper.setAgeType(ageType);

		String eligibility = wrapper.driverEligibility(driver, ageType);
		driverWrapper.setEligibility(eligibility);

		String risk = wrapper.driverRisk(driver);
		driverWrapper.setDriverRisk(risk);

	}

	private void printDriverEligibility(DriverCalc driverWrapper) {

		Driver driver = driverWrapper.getDriver();

		System.out.println("Driver: " + driver.getName());
		System.out.println("	Age type:    " + driverWrapper.getAgeType());
		System.out.println("	Risk:        " + driverWrapper.getDriverRisk());
		System.out.println("	Eligibility: " + driverWrapper.getEligibility());
	}

	private void calculatePolicyEligibility(Policy policy1,
			VehicleCalc[] vehicleWrappers1, DriverCalc[] driverWrappers1) {

		for (int i = 0; i < vehicleWrappers1.length; i++) {

			VehicleCalc vehicleWrapper = vehicleWrappers1[i];

			wrapper.setVehicleEligibilityScore(vehicleWrapper);
			policy1.addScore(vehicleWrapper.getFinalScore());
		}

		for (int i = 0; i < driverWrappers1.length; i++) {

			DriverCalc driverWrapper = driverWrappers1[i];

			DoubleValue driverTypeScore = wrapper.driverTypeScore(driverWrapper
					.getAgeType(), driverWrapper.getEligibility());
			policy1.addScore(driverTypeScore);

			DoubleValue driverRiskScore = wrapper.driverRiskScore(driverWrapper
					.getDriverRisk());
			policy1.addScore(driverRiskScore);
		}

		DoubleValue clientTierScoreAdjustment = wrapper
				.clientTierScoreAdjustment(policy1.getClientTier());
		policy1.addScore(clientTierScoreAdjustment);

		String policyEligibility = wrapper.policyEligibility(policy1);
		policy1.setEligibility(policyEligibility);
	}

	private void printPolicyEligibility(Policy policy) {

		System.out.println("Policy: " + policy.getName());
		System.out.println("	Final score: " + policy.getFinalScore());
		System.out.println("	Eligibility: " + policy.getEligibility());
	}

	private void calculateAutoPremium(VehicleCalc vehicleWrapper) {

		Vehicle vehicle = vehicleWrapper.getVehicle();

		DoubleValue basePrice = wrapper.basePrice(vehicle);
		DoubleValue price = basePrice;

		DoubleValue ageSurcharge = wrapper.ageSurcharge(vehicle);
		price = DoubleValue.add(price, ageSurcharge);

		DoubleValue coverageSurcharge = wrapper.coverageSurcharge(vehicle);
		price = DoubleValue.add(price, coverageSurcharge);

		DoubleValue injuryRatingSurcharge = wrapper
				.injuryRatingSurcharge(vehicleWrapper.getInjuryRating());
		price = DoubleValue.add(price, injuryRatingSurcharge);

		DoubleValue theftRatingSurcharge = wrapper
				.theftRatingSurcharge(vehicleWrapper.getTheftRating());
		price = DoubleValue.add(price, theftRatingSurcharge);

		vehicleWrapper.setPrice(price);

		wrapper.vehicleDiscount(vehicleWrapper);
	}

	private void printAutoPremium(VehicleCalc vehicleWrapper) {
		System.out.println("Vehicle: " + vehicleWrapper.getVehicle().getName());
		System.out.println("	Price:  " + vehicleWrapper.getPrice());
	}

	private void calculateDriverPremium(DriverCalc driverWrapper) {

		DoubleValue premium = wrapper.calcDriverPremium(driverWrapper);

		driverWrapper.setPremium(premium);
	}

	private void printDriverPremium(DriverCalc driverWrapper) {

		System.out.println("Driver: " + driverWrapper.getDriver().getName());
		System.out.println("	Premium:    " + driverWrapper.getPremium());
	}
}
