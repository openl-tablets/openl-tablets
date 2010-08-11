/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial5;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.OpenClassHelper;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;

public class Tutorial_5Wrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/Tutorial_5.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial5";

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

  public Tutorial_5Wrapper(){
    this(false);
  }

  public Tutorial_5Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField driverPremiumTest_Field;

  public org.openl.types.impl.DynamicObject[] getDriverPremiumTest()
  {
   Object __res = driverPremiumTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDriverPremiumTest(org.openl.types.impl.DynamicObject[] __var)
  {
   driverPremiumTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField ampmTo24Test_Field;

  public org.openl.types.impl.DynamicObject[] getAmpmTo24Test()
  {
   Object __res = ampmTo24Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setAmpmTo24Test(org.openl.types.impl.DynamicObject[] __var)
  {
   ampmTo24Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField ampmTo24Ind1Test_Field;

  public org.openl.types.impl.DynamicObject[] getAmpmTo24Ind1Test()
  {
   Object __res = ampmTo24Ind1Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setAmpmTo24Ind1Test(org.openl.types.impl.DynamicObject[] __var)
  {
   ampmTo24Ind1Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField largeTableTest_Field;

  public org.openl.types.impl.DynamicObject[] getLargeTableTest()
  {
   Object __res = largeTableTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setLargeTableTest(org.openl.types.impl.DynamicObject[] __var)
  {
   largeTableTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField regionIndTest_Field;

  public org.openl.types.impl.DynamicObject[] getRegionIndTest()
  {
   Object __res = regionIndTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRegionIndTest(org.openl.types.impl.DynamicObject[] __var)
  {
   regionIndTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField largeTableIndTest_Field;

  public org.openl.types.impl.DynamicObject[] getLargeTableIndTest()
  {
   Object __res = largeTableIndTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setLargeTableIndTest(org.openl.types.impl.DynamicObject[] __var)
  {
   largeTableIndTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField ampmTo24Ind2Test_Field;

  public org.openl.types.impl.DynamicObject[] getAmpmTo24Ind2Test()
  {
   Object __res = ampmTo24Ind2Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setAmpmTo24Ind2Test(org.openl.types.impl.DynamicObject[] __var)
  {
   ampmTo24Ind2Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverPremiumIndTest_Field;

  public org.openl.types.impl.DynamicObject[] getDriverPremiumIndTest()
  {
   Object __res = driverPremiumIndTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDriverPremiumIndTest(org.openl.types.impl.DynamicObject[] __var)
  {
   driverPremiumIndTest_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenField regionTest_Field;

  public org.openl.types.impl.DynamicObject[] getRegionTest()
  {
   Object __res = regionTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setRegionTest(org.openl.types.impl.DynamicObject[] __var)
  {
   regionTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenMethod ampmTo24Ind2_Method;
  public int ampmTo24Ind2(int ampmHr, java.lang.String ampm)  {
    Object[] __params = new Object[2];
    __params[0] = new Integer(ampmHr);
    __params[1] = ampm;
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24Ind2_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod regionTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult regionTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = regionTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod largeTableInd_Method;
  public int largeTableInd(int x)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(x);
    try
    {
    Object __myInstance = __instance;
    Object __res = largeTableInd_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod regionInd_Method;
  public java.lang.String regionInd(java.lang.String state)  {
    Object[] __params = new Object[1];
    __params[0] = state;
    try
    {
    Object __myInstance = __instance;
    Object __res = regionInd_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod largeTableIndTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult largeTableIndTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = largeTableIndTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ampmTo24TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult ampmTo24TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverPremiumIndTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult driverPremiumIndTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = driverPremiumIndTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod largeTableTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult largeTableTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = largeTableTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ampmTo24_Method;
  public int ampmTo24(int ampmHr, java.lang.String ampm)  {
    Object[] __params = new Object[2];
    __params[0] = new Integer(ampmHr);
    __params[1] = ampm;
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod largeTable_Method;
  public int largeTable(int x)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(x);
    try
    {
    Object __myInstance = __instance;
    Object __res = largeTable_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod region_Method;
  public java.lang.String region(java.lang.String state)  {
    Object[] __params = new Object[1];
    __params[0] = state;
    try
    {
    Object __myInstance = __instance;
    Object __res = region_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ampmTo24Ind1TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult ampmTo24Ind1TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24Ind1TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ampmTo24Ind1_Method;
  public int ampmTo24Ind1(int ampmHr, java.lang.String ampm)  {
    Object[] __params = new Object[2];
    __params[0] = new Integer(ampmHr);
    __params[1] = ampm;
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24Ind1_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverPremiumTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult driverPremiumTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = driverPremiumTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverPremium_Method;
  public org.openl.meta.DoubleValue driverPremium(java.lang.String state, java.lang.String driverAge, java.lang.String driverMS)  {
    Object[] __params = new Object[3];
    __params[0] = state;
    __params[1] = driverAge;
    __params[2] = driverMS;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod regionIndTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult regionIndTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = regionIndTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ampmTo24Ind2TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult ampmTo24Ind2TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = ampmTo24Ind2TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverPremiumInd_Method;
  public org.openl.meta.DoubleValue driverPremiumInd(java.lang.String state, java.lang.String driverAge, java.lang.String driverMS)  {
    Object[] __params = new Object[3];
    __params[0] = state;
    __params[1] = driverAge;
    __params[2] = driverMS;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverPremiumInd_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
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

    driverPremiumTest_Field = __class.getField("driverPremiumTest");
    ampmTo24Test_Field = __class.getField("ampmTo24Test");
    ampmTo24Ind1Test_Field = __class.getField("ampmTo24Ind1Test");
    largeTableTest_Field = __class.getField("largeTableTest");
    regionIndTest_Field = __class.getField("regionIndTest");
    largeTableIndTest_Field = __class.getField("largeTableIndTest");
    ampmTo24Ind2Test_Field = __class.getField("ampmTo24Ind2Test");
    driverPremiumIndTest_Field = __class.getField("driverPremiumIndTest");
    this_Field = __class.getField("this");
    regionTest_Field = __class.getField("regionTest");
    ampmTo24Ind2_Method = __class.getMatchingMethod("ampmTo24Ind2", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    regionTestTestAll_Method = __class.getMatchingMethod("regionTestTestAll", new IOpenClass[] {
});
    largeTableInd_Method = __class.getMatchingMethod("largeTableInd", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class)});
    regionInd_Method = __class.getMatchingMethod("regionInd", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    largeTableIndTestTestAll_Method = __class.getMatchingMethod("largeTableIndTestTestAll", new IOpenClass[] {
});
    ampmTo24TestTestAll_Method = __class.getMatchingMethod("ampmTo24TestTestAll", new IOpenClass[] {
});
    driverPremiumIndTestTestAll_Method = __class.getMatchingMethod("driverPremiumIndTestTestAll", new IOpenClass[] {
});
    largeTableTestTestAll_Method = __class.getMatchingMethod("largeTableTestTestAll", new IOpenClass[] {
});
    ampmTo24_Method = __class.getMatchingMethod("ampmTo24", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    largeTable_Method = __class.getMatchingMethod("largeTable", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class)});
    region_Method = __class.getMatchingMethod("region", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    ampmTo24Ind1TestTestAll_Method = __class.getMatchingMethod("ampmTo24Ind1TestTestAll", new IOpenClass[] {
});
    ampmTo24Ind1_Method = __class.getMatchingMethod("ampmTo24Ind1", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    driverPremiumTestTestAll_Method = __class.getMatchingMethod("driverPremiumTestTestAll", new IOpenClass[] {
});
    driverPremium_Method = __class.getMatchingMethod("driverPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    regionIndTestTestAll_Method = __class.getMatchingMethod("regionIndTestTestAll", new IOpenClass[] {
});
    ampmTo24Ind2TestTestAll_Method = __class.getMatchingMethod("ampmTo24Ind2TestTestAll", new IOpenClass[] {
});
    driverPremiumInd_Method = __class.getMatchingMethod("driverPremiumInd", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});

    __initialized=true;
  }
}