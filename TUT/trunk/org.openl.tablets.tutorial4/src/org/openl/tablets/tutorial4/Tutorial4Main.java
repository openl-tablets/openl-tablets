/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial4;

import org.openl.rules.calc.result.SpreadsheetResult;
import org.openl.types.impl.DynamicObject;
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
		DynamicObject[] policies = wrapper.getPolicyProfile1();
		DynamicObject policy = policies[0];

		calculatePolicyPremium(policy);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase2Example() {

		// Get policy profile.
		//
		DynamicObject[] policies = wrapper.getPolicyProfile2();
		DynamicObject policy = policies[0];

		calculatePolicyPremium(policy);
	}

	/**
	 * Preferred Client Business Rule V2. See '2005_Product_Derby.pdf' document
	 * for more details about tutorial use case.
	 */
	private void useCase3Example() {

		// Get policy profile.
		//
		DynamicObject[] policies = wrapper.getPolicyProfile3();
		DynamicObject policy = policies[0];

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
		DynamicObject[] policies = wrapper.getPolicyProfile4();
		DynamicObject policy = policies[0];

		calculatePolicyPremium(policy);
	}

	private void calculatePolicyPremium(DynamicObject policy) {

		SpreadsheetResult pc = wrapper.processPolicy(policy);

		System.out.println();
		System.out.println(String.format("'%s' premium calculation report.", 
				((DynamicObject)pc.getFieldValue("$Value$Policy")).getFieldValue("name")));
		System.out.println();

		SpreadsheetResult[] vehicles = (SpreadsheetResult[])pc.getFieldValue("$Value$Vehicles");
		System.out.println(" Vehicles:");
		
		for (SpreadsheetResult vc : vehicles) {
			System.out.println();
			System.out.println(" Vehicle: " + ((DynamicObject)vc.getFieldValue("$Value$Vehicle")).getFieldValue("name"));
			System.out.println("	Theft rating:   " + vc.getFieldValue("$Value$Theft Rating"));
			System.out.println("	Injury rating:  " + vc.getFieldValue("$Value$Injury Rating"));
			System.out.println("	Eligibility:    " + vc.getFieldValue("$Value$Eligibility"));
		}

		System.out.println();

		SpreadsheetResult[] drivers = (SpreadsheetResult[])pc.getFieldValue("$Value$Drivers");
		System.out.println(" Drivers:");
		
		for (SpreadsheetResult dc : drivers) {
			System.out.println();
			System.out.println(" Driver: " + ((DynamicObject)dc.getFieldValue("$Value$Driver")).getFieldValue("name"));
			System.out.println("	Age Type:    " + dc.getFieldValue("$Value$Age Type"));
			System.out.println("	Risk:        " + dc.getFieldValue("$Value$Driver Risk"));
			System.out.println("	Eligibility: " + dc.getFieldValue("$Value$Eligibility"));
		}

		System.out.println();
		System.out.println(" Result:");
		System.out.println("	Score:       " + pc.getFieldValue("$Value$Score"));
		System.out.println("	Eligibility: " + pc.getFieldValue("$Value$Eligibility"));
		System.out.println("	Premium:     " + pc.getFieldValue("$Value$Premium"));

		System.out.println();
		System.out.println();
	}
}
