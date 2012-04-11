package org.openl.rules.tablets.tutorial4.client;

import org.openl.tablets.tutorial4.Driver;

/**
 * Web app is supposed to call web service via an instance of this interface.
 */
public interface Tutorial4ClientInterface {
	String[] getCoverage();
	String[] getTheft_rating();
	String driverAgeType(Driver driver);
}
