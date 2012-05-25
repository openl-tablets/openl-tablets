/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial7;


public interface Tutorial_7RulesInterface {
  public static java.lang.String __src = "rules/Tutorial_7.xls";


  public org.openl.types.impl.DynamicObject[] getTest1();


  public org.openl.types.impl.DynamicObject getThis();


  public org.openl.types.impl.DynamicObject[] getTest2();


  public org.openl.types.impl.DynamicObject[] getTest3();

  org.openl.rules.testmethod.TestUnitsResults test1TestAll();

  org.openl.rules.testmethod.TestUnitsResults test2TestAll();

  java.lang.String needApprovalOf(org.openl.tablets.tutorial7.Expense expense);

  org.openl.rules.testmethod.TestUnitsResults test3TestAll();

  java.lang.String scoreIssueImportance(org.openl.tablets.tutorial7.Issue issue);

  int scoreIssue(org.openl.tablets.tutorial7.Issue issue);

}