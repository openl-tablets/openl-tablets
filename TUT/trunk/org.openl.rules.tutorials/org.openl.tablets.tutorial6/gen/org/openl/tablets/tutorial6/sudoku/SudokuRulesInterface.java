/*
 * This class has been generated. 
*/

package org.openl.tablets.tutorial6.sudoku;


public interface SudokuRulesInterface {
  public static java.lang.String __src = "rules/SudokuRules3.xls";


  public org.openl.types.impl.DynamicObject[] getRunSudoku();


  public org.openl.types.impl.DynamicObject getThis();

  void sq1(java.util.Vector v);

  void s3(java.util.Vector v);

  void s2(java.util.Vector v);

  java.lang.Object run(java.lang.String sname, boolean display);

  void s1(java.util.Vector v);

  void s34b(java.util.Vector v);

  org.openl.tablets.tutorial6.sudoku.SudokuSolver solver(java.lang.String sname, int H, int W, int[][] data);

  org.openl.rules.testmethod.TestUnitsResults runSudokuTestAll();

  void s34a(java.util.Vector v);

  int[][] solve(java.util.Vector v, java.lang.String sname);

  void s4(java.util.Vector v);

}