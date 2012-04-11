/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial9;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.impl.ISyntaxConstants;
public class Tutorial_9Wrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_9.xls";

  public static String __srcModuleClass = null;

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial9";

  public static String __userHome = ".";

  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
    @Override
    protected org.openl.vm.IRuntimeEnv initialValue() {
      return new org.openl.vm.SimpleVM().getRuntimeEnv();
    }
  };

  public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {
    return __env.get();
  }

  public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv environment) {
    __env.set(environment);
  }

  public Tutorial_9Wrapper(){
    this(false);
  }

  public Tutorial_9Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField run1_Field;

  public org.openl.types.impl.DynamicObject[] getRun1()
  {
   Object __res = run1_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRun1(org.openl.types.impl.DynamicObject[] __var)
  {
   run1_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenMethod run1TestAll_Method;
  public org.openl.rules.testmethod.TestResult run1TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = run1TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod incomeForecast_Method;
  public org.openl.rules.calc.result.SpreadsheetResult incomeForecast(double bonusRate, double sharePrice)  {
    Object[] __params = new Object[2];
    __params[0] = new Double(bonusRate);
    __params[1] = new Double(sharePrice);
    try
    {
    Object __myInstance = __instance;
    Object __res = incomeForecast_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.result.SpreadsheetResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod totalAssets_Method;
  public org.openl.rules.calc.result.SpreadsheetResult totalAssets()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = totalAssets_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.result.SpreadsheetResult)__res;  }
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
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src, __srcModuleClass);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env.set(wrapper.getEnv());

    run1_Field = __class.getField("run1");
    this_Field = __class.getField("this");
    run1TestAll_Method = __class.getMatchingMethod("run1TestAll", new IOpenClass[] {
});
    incomeForecast_Method = __class.getMatchingMethod("incomeForecast", new IOpenClass[] {
      JavaOpenClass.getOpenClass(double.class),
      JavaOpenClass.getOpenClass(double.class)});
    totalAssets_Method = __class.getMatchingMethod("totalAssets", new IOpenClass[] {
});

    __initialized=true;
  }
}