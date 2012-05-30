/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial6.sudoku;


public interface SudokuRulesInterface {
  public static java.lang.String __src = "rules/SudokuRules3.xls";


  public org.openl.types.impl.DynamicObject[] getRunSudoku();


  public org.openl.types.impl.DynamicObject getThis();

  int[][] solve(java.util.Vector v, java.lang.String sname);

  void s34b(java.util.Vector v);

  void s4(java.util.Vector v);

  org.openl.tablets.tutorial6.sudoku.SudokuSolver solver(java.lang.String sname, int H, int W, int[][] data);

  java.lang.Object run(java.lang.String sname, boolean display);

  void s3(java.util.Vector v);

  void s34a(java.util.Vector v);

  org.openl.rules.testmethod.TestUnitsResults runSudokuTestAll();

  void sq1(java.util.Vector v);

  void s2(java.util.Vector v);

  void s1(java.util.Vector v);

}