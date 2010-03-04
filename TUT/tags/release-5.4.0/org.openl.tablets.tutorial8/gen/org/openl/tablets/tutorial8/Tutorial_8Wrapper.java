/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial8;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class Tutorial_8Wrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_8.xls";

  public static String __srcModuleClass = null;

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial8";

  public static String __userHome = ".";

  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
    @Override
    protected org.openl.vm.IRuntimeEnv initialValue() {
      this.set(new org.openl.vm.SimpleVM().getRuntimeEnv());
      return this.get();
    }
  };

  public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {
    return __env.get();
  }

  public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv __env) {
    this.__env.set(__env);
  }

  public Tutorial_8Wrapper(){
    this(false);
  }

  public Tutorial_8Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField run3_Field;

  public org.openl.types.impl.DynamicObject[] getRun3()
  {
   Object __res = run3_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRun3(org.openl.types.impl.DynamicObject[] __var)
  {
   run3_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField test1_Field;

  public org.openl.types.impl.DynamicObject[] getTest1()
  {
   Object __res = test1_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setTest1(org.openl.types.impl.DynamicObject[] __var)
  {
   test1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField run2_Field;

  public org.openl.types.impl.DynamicObject[] getRun2()
  {
   Object __res = run2_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRun2(org.openl.types.impl.DynamicObject[] __var)
  {
   run2_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenMethod test1TestAll_Method;
  public org.openl.rules.testmethod.TestResult test1TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test1TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod totalToPay_Method;
  public double totalToPay(org.openl.tablets.tutorial8.Loan loan)  {
    Object[] __params = new Object[1];
    __params[0] = loan;
    try
    {
    Object __myInstance = __instance;
    Object __res = totalToPay_Method.invoke(__myInstance, __params, __env.get());
   return ((Double)__res).doubleValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod run2TestAll_Method;
  public org.openl.rules.testmethod.TestResult run2TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = run2TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod totalPayments_Method;
  public double totalPayments(org.openl.tablets.tutorial8.Payments payments)  {
    Object[] __params = new Object[1];
    __params[0] = payments;
    try
    {
    Object __myInstance = __instance;
    Object __res = totalPayments_Method.invoke(__myInstance, __params, __env.get());
   return ((Double)__res).doubleValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod listPayments_Method;
  public org.openl.tablets.tutorial8.Payments listPayments(org.openl.tablets.tutorial8.Loan loan)  {
    Object[] __params = new Object[1];
    __params[0] = loan;
    try
    {
    Object __myInstance = __instance;
    Object __res = listPayments_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.tablets.tutorial8.Payments)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod factorial_Method;
  public int factorial(int n)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(n);
    try
    {
    Object __myInstance = __instance;
    Object __res = factorial_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod run3TestAll_Method;
  public org.openl.rules.testmethod.TestResult run3TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = run3TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
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

    run3_Field = __class.getField("run3");
    test1_Field = __class.getField("test1");
    run2_Field = __class.getField("run2");
    this_Field = __class.getField("this");
    test1TestAll_Method = __class.getMatchingMethod("test1TestAll", new IOpenClass[] {
});
    totalToPay_Method = __class.getMatchingMethod("totalToPay", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial8.Loan.class)});
    run2TestAll_Method = __class.getMatchingMethod("run2TestAll", new IOpenClass[] {
});
    totalPayments_Method = __class.getMatchingMethod("totalPayments", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial8.Payments.class)});
    listPayments_Method = __class.getMatchingMethod("listPayments", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial8.Loan.class)});
    factorial_Method = __class.getMatchingMethod("factorial", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class)});
    run3TestAll_Method = __class.getMatchingMethod("run3TestAll", new IOpenClass[] {
});

    __initialized=true;
  }
}