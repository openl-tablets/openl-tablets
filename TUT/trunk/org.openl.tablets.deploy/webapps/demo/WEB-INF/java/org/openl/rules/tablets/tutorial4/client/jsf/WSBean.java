package org.openl.rules.tablets.tutorial4.client.jsf;

import org.openl.rules.tablets.tutorial4.client.Tutorial4ClientInterface;
import org.openl.tablets.tutorial4.Driver;

public class WSBean {
	private String[] result;
	private String methodName;
	private int param1;
	private String param2;

	public String[] getResult() {
		return result;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getParam1() {
		return param1;
	}

	public void setParam1(int param1) {
		this.param1 = param1;
	}

	public String getParam2() {
		return param2;
	}

	public void setParam2(String param2) {
		this.param2 = param2;
	}

	public void getCoverage() {
		methodName = "getCoverage"; 
		result = new AbstractBean<String[]>() {
			public String[] perform(Tutorial4ClientInterface client) {
				return client.getCoverage();
			}
		}.getResult();
	}

	public void getTheftRating() {
		methodName = "getTheft_rating";
		result = new AbstractBean<String[]>() {
			public String[] perform(Tutorial4ClientInterface client) {
				return client.getTheft_rating();
			}
		}.getResult();
	}

	public void driverAgeType() {
		methodName = "driverAgeType";

		final Driver driver = new Driver();
		driver.setAge(param1);
		driver.setGender(param2);

		String ret = new AbstractBean<String>() {
			public String perform(Tutorial4ClientInterface client) {
				return client.driverAgeType(driver);
			}
		}.getResult();

		result = new String[] {ret};
	}
}
