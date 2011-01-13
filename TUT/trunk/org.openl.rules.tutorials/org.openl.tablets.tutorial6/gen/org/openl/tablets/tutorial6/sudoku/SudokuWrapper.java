/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial6.sudoku;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.OpenClassHelper;
import java.util.Map;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.dependency.IDependencyManager;

public class SudokuWrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static IDependencyManager __dependencyManager;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/SudokuRules3.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial6";

  public static java.lang.String __userHome = ".";

  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
    @Override
    protected org.openl.vm.IRuntimeEnv initialValue() {
      org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
      environment.setContext(new org.openl.rules.context.DefaultRulesRuntimeContext());
      return environment;
    }
  };

  public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {
    return __env.get();
  }

  public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv environment) {
    __env.set(environment);
  }

  public org.openl.rules.context.IRulesRuntimeContext getRuntimeContext() {
    return (org.openl.rules.context.IRulesRuntimeContext)getRuntimeEnvironment().getContext();
  }

  public void setRuntimeContext(org.openl.rules.context.IRulesRuntimeContext context) {
    getRuntimeEnvironment().setContext(context);
  }

  public SudokuWrapper(){
    this(false);
  }

  public SudokuWrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public SudokuWrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public SudokuWrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public SudokuWrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    this(ignoreErrors, executionMode, params, null);
  }

  public SudokuWrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params, IDependencyManager dependencyManager){
    __externalParams = params;
    __executionMode = executionMode;
    __dependencyManager = dependencyManager;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField runSudoku_Field;

  public org.openl.types.impl.DynamicObject[] getRunSudoku()
  {
   Object __res = runSudoku_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRunSudoku(org.openl.types.impl.DynamicObject[] __var)
  {
   runSudoku_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField this_Field;

  public org.openl.types.impl.DynamicObject getThis()
  {
   Object __res = this_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject)__res;
  }


  public void setThis(org.openl.types.impl.DynamicObject __var)
  {
   this_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenMethod solve_Method;
  public int[][] solve(java.util.Vector v, java.lang.String sname)  {
    Object[] __params = new Object[2];
    __params[0] = v;
    __params[1] = sname;
    try
    {
    Object __myInstance = __instance;
    Object __res = solve_Method.invoke(__myInstance, __params, __env.get());
   return (int[][])__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s34a_Method;
  public void s34a(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s34a_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod sq1_Method;
  public void sq1(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    sq1_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod run_Method;
  public java.lang.Object run(java.lang.String sname, boolean display)  {
    Object[] __params = new Object[2];
    __params[0] = sname;
    __params[1] = new Boolean(display);
    try
    {
    Object __myInstance = __instance;
    Object __res = run_Method.invoke(__myInstance, __params, __env.get());
   return __res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod solver_Method;
  public org.openl.tablets.tutorial6.sudoku.SudokuSolver solver(java.lang.String sname, int H, int W, int[][] data)  {
    Object[] __params = new Object[4];
    __params[0] = sname;
    __params[1] = new Integer(H);
    __params[2] = new Integer(W);
    __params[3] = data;
    try
    {
    Object __myInstance = __instance;
    Object __res = solver_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.tablets.tutorial6.sudoku.SudokuSolver)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s3_Method;
  public void s3(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s3_Method.invoke(__myInstance, __params, __env.get());  }
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
    s2_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s4_Method;
  public void s4(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s4_Method.invoke(__myInstance, __params, __env.get());  }
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
    s1_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod runSudokuTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults runSudokuTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = runSudokuTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod s34b_Method;
  public void s34b(java.util.Vector v)  {
    Object[] __params = new Object[1];
    __params[0] = v;
    try
    {
    Object __myInstance = __instance;
    s34b_Method.invoke(__myInstance, __params, __env.get());  }
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

public synchronized void  reload(){reset();__init();__instance = __class.newInstance(__env.get());}

  static synchronized protected void __init()
  {
    if (__initialized)
      return;

    IUserContext ucxt = UserContext.makeOrLoadContext(Thread.currentThread().getContextClassLoader(), __userHome);
    IOpenSourceCodeModule source = OpenClassJavaWrapper.getSourceCodeModule(__src, ucxt);
    if (source != null) {
         source.setParams(__externalParams);
    }
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , source, __executionMode, __dependencyManager);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env.set(wrapper.getEnv());

    runSudoku_Field = __class.getField("runSudoku");
    this_Field = __class.getField("this");
    solve_Method = __class.getMatchingMethod("solve", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    s34a_Method = __class.getMatchingMethod("s34a", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    sq1_Method = __class.getMatchingMethod("sq1", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    run_Method = __class.getMatchingMethod("run", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, boolean.class)});
    solver_Method = __class.getMatchingMethod("solver", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, int[][].class)});
    s3_Method = __class.getMatchingMethod("s3", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    s2_Method = __class.getMatchingMethod("s2", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    s4_Method = __class.getMatchingMethod("s4", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    s1_Method = __class.getMatchingMethod("s1", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});
    runSudokuTestAll_Method = __class.getMatchingMethod("runSudokuTestAll", new IOpenClass[] {
});
    s34b_Method = __class.getMatchingMethod("s34b", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class)});

    __initialized=true;
  }
}