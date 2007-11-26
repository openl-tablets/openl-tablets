/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial6.sudoku;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class SudokuWrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

    public org.openl.vm.IRuntimeEnv __env = new org.openl.vm.SimpleVM().getRuntimeEnv();

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/SudokuRules.xls";

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial6";

  public static String __userHome = ".";

  public SudokuWrapper(){
    this(false);
  }

  public SudokuWrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env);
  }



  static org.openl.types.IOpenField runSudoku_Field;

  public org.openl.types.impl.DynamicObject[] getRunSudoku()
  {
   Object __res = runSudoku_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRunSudoku(org.openl.types.impl.DynamicObject[] __var)
  {
   runSudoku_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField displaySudoku_Field;

  public org.openl.types.impl.DynamicObject[] getDisplaySudoku()
  {
   Object __res = displaySudoku_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDisplaySudoku(org.openl.types.impl.DynamicObject[] __var)
  {
   displaySudoku_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField this_Field;

  public org.openl.types.impl.DynamicObject getThis()
  {
   Object __res = this_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject)__res;
  }


  public void setThis(org.openl.types.impl.DynamicObject __var)
  {
   this_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenMethod displaySudokuTestAll_Method;
  public org.openl.rules.testmethod.TestResult displaySudokuTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = displaySudokuTestAll_Method.invoke(__myInstance, __params, __env);
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod run_Method;
  public int[][] run(int n)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(n);
    try
    {
    Object __myInstance = __instance;
    Object __res = run_Method.invoke(__myInstance, __params, __env);
   return (int[][])__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod runSudokuTestAll_Method;
  public org.openl.rules.testmethod.TestResult runSudokuTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = runSudokuTestAll_Method.invoke(__myInstance, __params, __env);
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s2_Method;
  public void s2(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s2_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s1_Method;
  public void s1(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s1_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod display_Method;
  public java.lang.Object display(int n)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(n);
    try
    {
    Object __myInstance = __instance;
    Object __res = display_Method.invoke(__myInstance, __params, __env);
   return __res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }
  static boolean __initialized = false;

  static public void reset(){__initialized = false;}

public Object getInstance(){return __instance;}

public IOpenClass getOpenClass(){return __class;}

public org.openl.CompiledOpenClass getCompiledOpenClass(){return __compiledClass;}

public synchronized void  reload(){reset();__init();__instance = __class.newInstance(__env);}

  static synchronized protected void __init()
  {
    if (__initialized)
      return;

    UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), __userHome);
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env = wrapper.getEnv();

    runSudoku_Field = __class.getField("runSudoku");
    displaySudoku_Field = __class.getField("displaySudoku");
    this_Field = __class.getField("this");
    displaySudokuTestAll_Method = __class.getMatchingMethod("displaySudokuTestAll", new IOpenClass[] {
});
    run_Method = __class.getMatchingMethod("run", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class)});
    runSudokuTestAll_Method = __class.getMatchingMethod("runSudokuTestAll", new IOpenClass[] {
});
    s2_Method = __class.getMatchingMethod("s2", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.util.Vector.class)});
    s1_Method = __class.getMatchingMethod("s1", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.util.Vector.class)});
    display_Method = __class.getMatchingMethod("display", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class)});

    __initialized=true;
  }
}