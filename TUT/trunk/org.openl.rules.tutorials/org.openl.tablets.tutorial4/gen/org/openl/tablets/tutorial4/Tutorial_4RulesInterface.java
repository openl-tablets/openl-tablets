/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial4;


public interface Tutorial_4RulesInterface {
  public static java.lang.String __src = "rules/main/Tutorial_4.xls";


  public java.lang.String[] getDriverRisk();


  public org.openl.types.impl.DynamicObject[] getDriverEligibilityTest();


  public java.lang.String[] getMaritalStatus();


  public java.lang.String[] getTheft_rating();


  public org.openl.rules.table.properties.TableProperties getCatVehiclePremium();


  public java.lang.String[] getCar_type();


  public org.openl.types.impl.DynamicObject[] getVehicleInjuryRatingTest();


  public org.openl.generated.beans.Policy[] getPolicyProfile2();


  public org.openl.generated.beans.Policy[] getPolicyProfile1();


  public java.lang.String[] getAirbag_type();


  public org.openl.types.impl.DynamicObject[] getDriverRiskTest();


  public org.openl.generated.beans.Policy[] getPolicyProfile4();


  public java.lang.String[] getInjury_rating();


  public org.openl.generated.beans.Policy[] getPolicyProfile3();


  public java.lang.String[] getGender();


  public org.openl.generated.beans.Driver[] getDriverProfiles1();


  public org.openl.generated.beans.Vehicle[] getTestVehicles1();


  public org.openl.generated.beans.Driver[] getTestDrivers1();


  public org.openl.generated.beans.Driver[] getDriverProfiles2();


  public org.openl.rules.table.properties.TableProperties getModuleProp();


  public org.openl.generated.beans.Driver[] getDriverProfiles3();


  public org.openl.generated.beans.Policy[] getTestPolicy2();


  public org.openl.types.impl.DynamicObject[] getVehicleTheftRatingTest();


  public org.openl.generated.beans.Policy[] getTestPolicy1();


  public org.openl.generated.beans.Vehicle[] getAutoProfiles2();


  public org.openl.rules.table.properties.TableProperties getCatPolicyScoring();


  public org.openl.generated.beans.Vehicle[] getAutoProfiles1();


  public org.openl.generated.beans.Vehicle[] getAutoProfiles3();


  public java.lang.String[] getEligibility_type();


  public java.lang.String[] getCoverage();


  public org.openl.types.impl.DynamicObject[] getDriverAgeTypeTest();


  public java.lang.String[] getDriver_type();


  public java.lang.String[] getClientTier();


  public org.openl.types.impl.DynamicObject getThis();

  org.openl.meta.DoubleValue calculateDriversScore(java.lang.Object drivers);

  org.openl.meta.DoubleValue driverTypeScore(java.lang.String driverAgeType, java.lang.String driverEligibility);

  java.lang.String vehicleInjuryRating(org.openl.generated.beans.Vehicle vehicle);

  org.openl.meta.DoubleValue theftRatingSurcharge(java.lang.String theftRating);

  org.openl.meta.DoubleValue basePrice(org.openl.generated.beans.Vehicle vehicle);

  org.openl.meta.DoubleValue coverageSurcharge(org.openl.generated.beans.Vehicle vehicle);

  org.openl.meta.DoubleValue injuryRatingSurcharge(java.lang.String injuryRating);

  java.lang.String policyEligibility(org.openl.generated.beans.Policy policy, int score);

  java.lang.String driverEligibility(org.openl.generated.beans.Driver driver, java.lang.String ageType);

  org.openl.meta.DoubleValue driverRiskScore(java.lang.String driverRisk);

  org.openl.rules.calc.SpreadsheetResult processDriver(org.openl.generated.beans.Driver driver);

  org.openl.rules.testmethod.TestUnitsResults driverAgeTypeTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults driverEligibilityTestTestAll();

  org.openl.meta.DoubleValue vehicleDiscount(org.openl.generated.beans.Vehicle vehicle, java.lang.String vehicleTheftRating);

  org.openl.meta.DoubleValue vehicleEligibilityScore(java.lang.String vehicleEligibility);

  org.openl.rules.calc.SpreadsheetResult processPolicy(org.openl.generated.beans.Policy policy);

  org.openl.meta.DoubleValue ageSurcharge(int vehicleAge);

  org.openl.rules.calc.SpreadsheetResult processVehicle(org.openl.generated.beans.Vehicle vehicle);

  int currentYear();

  org.openl.meta.DoubleValue clientDiscount(org.openl.generated.beans.Policy policy);

  org.openl.rules.testmethod.TestUnitsResults vehicleInjuryRatingTestTestAll();

  org.openl.meta.DoubleValue clientTierScore(org.openl.generated.beans.Policy policy);

  org.openl.rules.testmethod.TestUnitsResults vehicleTheftRatingTestTestAll();

  org.openl.meta.DoubleValue driverAccidentPremium(org.openl.generated.beans.Driver driver, java.lang.String driverRisk);

  org.openl.rules.calc.SpreadsheetResult[] processVehicles(org.openl.generated.beans.Vehicle[] vehicles);

  java.lang.String vehicleEligibility(java.lang.String vehicleTheftRating, java.lang.String vehicleInjuryRating);

  org.openl.meta.DoubleValue calculateVehiclesScore(java.lang.Object vehicles);

  org.openl.meta.DoubleValue driverRiskPremium(java.lang.String driverRisk);

  org.openl.meta.DoubleValue calculateDriversPremium(java.lang.Object drivers);

  java.lang.String driverAgeType(org.openl.generated.beans.Driver driver);

  java.lang.String vehicleTheftRating(org.openl.generated.beans.Vehicle vehicle);

  org.openl.rules.calc.SpreadsheetResult[] processDrivers(org.openl.generated.beans.Driver[] drivers);

  org.openl.meta.DoubleValue driverPremium(org.openl.generated.beans.Driver driver, java.lang.String driverAgeType);

  org.openl.rules.testmethod.TestUnitsResults driverRiskTestTestAll();

  org.openl.meta.DoubleValue calculateVehiclesPremium(java.lang.Object vehicles);

  java.lang.String driverRisk(org.openl.generated.beans.Driver driver);

}