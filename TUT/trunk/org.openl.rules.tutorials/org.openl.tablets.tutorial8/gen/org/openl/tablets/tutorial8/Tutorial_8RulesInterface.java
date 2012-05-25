/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial8;


public interface Tutorial_8RulesInterface {
  public static java.lang.String __src = "rules/Tutorial_8.xls";


  public org.openl.types.impl.DynamicObject[] getRun2();


  public org.openl.types.impl.DynamicObject[] getTest1();


  public org.openl.types.impl.DynamicObject getThis();


  public org.openl.types.impl.DynamicObject[] getRun3();

  org.openl.rules.testmethod.TestUnitsResults test1TestAll();

  double totalToPay(org.openl.tablets.tutorial8.Loan loan);

  double totalPayments(org.openl.tablets.tutorial8.Payments payments);

  int factorial(int n);

  org.openl.rules.testmethod.TestUnitsResults run3TestAll();

  org.openl.tablets.tutorial8.Payments listPayments(org.openl.tablets.tutorial8.Loan loan);

  org.openl.rules.testmethod.TestUnitsResults run2TestAll();

}