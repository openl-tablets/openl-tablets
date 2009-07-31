/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

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
		Policy policy = policies[0];

		calculatePolicyPremium(policy);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase2Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile2();
		Policy policy = policies[0];

		calculatePolicyPremium(policy);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase3Example() {

		// Get policy profile.
		//
		Policy[] policies = wrapper.getPolicyProfile3();
		Policy policy = policies[0];

		calculatePolicyPremium(policy);
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
		Policy policy = policies[0];

		calculatePolicyPremium(policy);
	}

	private void calculatePolicyPremium(Policy policy) {

		PolicyCalc pc = new PolicyCalc(policy);
		wrapper.calcPolicyPremium(pc);

		System.out.println();
		System.out.println(String.format("'%s' premium calculation report.", pc
				.getPolicy().getName()));
		System.out.println();

		System.out.println(" Vehicles:");
		for (VehicleCalc vc : pc.getVehicleCalcs()) {
			System.out.println();
			System.out.println(" Vehicle: " + vc.getVehicle().getName());
			System.out.println("	Theft rating:   " + vc.getTheftRating());
			System.out.println("	Injury rating:  " + vc.getInjuryRating());
			System.out.println("	Eligibility:    " + vc.getEligibility());
		}

		System.out.println();

		System.out.println(" Drivers:");
		for (DriverCalc dc : pc.getDriverCalcs()) {
			System.out.println();
			System.out.println(" Driver: " + dc.getDriver().getName());
			System.out.println("	Age Type:    " + dc.getAgeType());
			System.out.println("	Risk:        " + dc.getDriverRisk());
			System.out.println("	Eligibility: " + dc.getEligibility());
		}

		System.out.println();
		System.out.println(" Result:");
		System.out.println("	Score:       " + pc.getScore());
		System.out.println("	Eligibility: " + pc.getEligibility());
		System.out.println("	Premium:     " + pc.getPremium());

		System.out.println();
		System.out.println();
	}
}
