/*
 * This class has been generated. 
*/

package org.openl.eclipse.interface.based;


public interface MainRulesInterface {
  public static java.lang.String __src = "rules/main/MainRules.xls";

  org.openl.meta.DoubleValue calculateDriversScore(java.lang.Object drivers);

  org.openl.meta.DoubleValue driverRiskPremium(java.lang.String driverRisk);

  org.openl.meta.DoubleValue driverTypeScore(java.lang.String driverAgeType, java.lang.String driverEligibility);

  org.openl.meta.DoubleValue clientDiscount(org.openl.eclipse.interface.based.Policy policy);

  org.openl.meta.DoubleValue driverRiskScore(java.lang.String driverRisk);

  java.lang.String driverEligibility(org.openl.eclipse.interface.based.Driver driver, java.lang.String ageType);

  org.openl.meta.DoubleValue injuryRatingSurcharge(java.lang.String injuryRating);

  org.openl.rules.testmethod.TestUnitsResults driverEligibilityTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults driverAgeTypeTestTestAll();

  org.openl.meta.DoubleValue calculateDriversPremium(java.lang.Object drivers);

  org.openl.meta.DoubleValue driverPremium(org.openl.eclipse.interface.based.Driver driver, java.lang.String driverAgeType);

  org.openl.meta.DoubleValue ageSurcharge(int vehicleAge);

  org.openl.meta.DoubleValue basePrice(org.openl.eclipse.interface.based.Vehicle vehicle);

  java.lang.String driverAgeType(org.openl.eclipse.interface.based.Driver driver);

  java.lang.String policyEligibility(org.openl.eclipse.interface.based.Policy policy, int score);

  org.openl.rules.calc.SpreadsheetResult[] processVehicles(org.openl.eclipse.interface.based.Vehicle[] vehicles);

  org.openl.meta.DoubleValue calculateVehiclesPremium(java.lang.Object vehicles);

  org.openl.meta.DoubleValue vehicleDiscount(org.openl.eclipse.interface.based.Vehicle vehicle, java.lang.String vehicleTheftRating);

  org.openl.meta.DoubleValue theftRatingSurcharge(java.lang.String theftRating);

  int currentYear();

  org.openl.meta.DoubleValue driverAccidentPremium(org.openl.eclipse.interface.based.Driver driver, java.lang.String driverRisk);

  org.openl.rules.testmethod.TestUnitsResults vehicleInjuryRatingTestTestAll();

  java.lang.String vehicleInjuryRating(org.openl.eclipse.interface.based.Vehicle vehicle);

  org.openl.meta.DoubleValue clientTierScore(org.openl.eclipse.interface.based.Policy policy);

  org.openl.rules.calc.SpreadsheetResult processDriver(org.openl.eclipse.interface.based.Driver driver);

  org.openl.rules.testmethod.TestUnitsResults vehicleTheftRatingTestTestAll();

  java.lang.String vehicleEligibility(java.lang.String vehicleTheftRating, java.lang.String vehicleInjuryRating);

  org.openl.rules.calc.SpreadsheetResult processPolicy(org.openl.eclipse.interface.based.Policy policy);

  org.openl.meta.DoubleValue vehicleEligibilityScore(java.lang.String vehicleEligibility);

  org.openl.rules.calc.SpreadsheetResult[] processDrivers(org.openl.eclipse.interface.based.Driver[] drivers);

  org.openl.meta.DoubleValue calculateVehiclesScore(java.lang.Object vehicles);

  java.lang.String vehicleTheftRating(org.openl.eclipse.interface.based.Vehicle vehicle);

  org.openl.rules.calc.SpreadsheetResult processVehicle(org.openl.eclipse.interface.based.Vehicle vehicle);

  java.lang.String driverRisk(org.openl.eclipse.interface.based.Driver driver);

  org.openl.meta.DoubleValue coverageSurcharge(org.openl.eclipse.interface.based.Vehicle vehicle);

  org.openl.rules.testmethod.TestUnitsResults driverRiskTestTestAll();

}