/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial5;


public interface Tutorial_5RulesInterface {
  public static java.lang.String __src = "rules/Tutorial_5.xls";


  public org.openl.types.impl.DynamicObject[] getDriverPremiumTest();


  public org.openl.types.impl.DynamicObject[] getAmpmTo24Test();


  public org.openl.types.impl.DynamicObject[] getLargeTableTest();


  public org.openl.types.impl.DynamicObject[] getAmpmTo24Ind1Test();


  public org.openl.types.impl.DynamicObject[] getRegionIndTest();


  public org.openl.types.impl.DynamicObject[] getLargeTableIndTest();


  public org.openl.types.impl.DynamicObject[] getDriverPremiumIndTest();


  public org.openl.types.impl.DynamicObject[] getAmpmTo24Ind2Test();


  public org.openl.types.impl.DynamicObject getThis();


  public org.openl.types.impl.DynamicObject[] getRegionTest();

  int ampmTo24(int ampmHr, java.lang.String ampm);

  org.openl.rules.testmethod.TestUnitsResults regionTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults ampmTo24TestTestAll();

  org.openl.rules.testmethod.TestUnitsResults largeTableIndTestTestAll();

  org.openl.meta.DoubleValue driverPremium(java.lang.String state, java.lang.String driverAge, java.lang.String driverMS);

  int ampmTo24Ind1(int ampmHr, java.lang.String ampm);

  int largeTableInd(int x);

  org.openl.rules.testmethod.TestUnitsResults driverPremiumIndTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults largeTableTestTestAll();

  org.openl.meta.DoubleValue driverPremiumInd(java.lang.String state, java.lang.String driverAge, java.lang.String driverMS);

  int largeTable(int x);

  org.openl.rules.testmethod.TestUnitsResults ampmTo24Ind1TestTestAll();

  int ampmTo24Ind2(int ampmHr, java.lang.String ampm);

  java.lang.String region(java.lang.String state);

  org.openl.rules.testmethod.TestUnitsResults driverPremiumTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults regionIndTestTestAll();

  org.openl.rules.testmethod.TestUnitsResults ampmTo24Ind2TestTestAll();

  java.lang.String regionInd(java.lang.String state);

}