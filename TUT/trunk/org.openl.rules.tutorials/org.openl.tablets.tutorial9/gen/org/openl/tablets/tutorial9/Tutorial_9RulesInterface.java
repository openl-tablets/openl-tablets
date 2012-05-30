/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial9;


public interface Tutorial_9RulesInterface {
  public static java.lang.String __src = "rules/Tutorial_9.xls";


  public org.openl.types.impl.DynamicObject[] getRun1();


  public org.openl.types.impl.DynamicObject getThis();

  org.openl.rules.calc.SpreadsheetResult incomeForecast(double bonusRate, double sharePrice);

  org.openl.rules.testmethod.TestUnitsResults run1TestAll();

  org.openl.rules.calc.SpreadsheetResult totalAssets();

}