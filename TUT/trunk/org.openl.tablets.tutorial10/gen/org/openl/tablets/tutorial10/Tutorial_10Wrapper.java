/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial10;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class Tutorial_10Wrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_10.xls";

  public static String __srcModuleClass = null;

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial10";

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

  public Tutorial_10Wrapper(){
    this(false);
  }

  public Tutorial_10Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField getPriceForOrder2010Test_Field;

  public org.openl.types.impl.DynamicObject[] getGetPriceForOrder2010Test()
  {
   Object __res = getPriceForOrder2010Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setGetPriceForOrder2010Test(org.openl.types.impl.DynamicObject[] __var)
  {
   getPriceForOrder2010Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField getCarPrice2010Test_Field;

  public org.openl.types.impl.DynamicObject[] getGetCarPrice2010Test()
  {
   Object __res = getCarPrice2010Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setGetCarPrice2010Test(org.openl.types.impl.DynamicObject[] __var)
  {
   getCarPrice2010Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField getDiscountPercentageTest_Field;

  public org.openl.types.impl.DynamicObject[] getGetDiscountPercentageTest()
  {
   Object __res = getDiscountPercentageTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setGetDiscountPercentageTest(org.openl.types.impl.DynamicObject[] __var)
  {
   getDiscountPercentageTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField getPriceForOrder2009Test_Field;

  public org.openl.types.impl.DynamicObject[] getGetPriceForOrder2009Test()
  {
   Object __res = getPriceForOrder2009Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setGetPriceForOrder2009Test(org.openl.types.impl.DynamicObject[] __var)
  {
   getPriceForOrder2009Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testCars_Field;

  public org.openl.tablets.tutorial10.Car[] getTestCars()
  {
   Object __res = testCars_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial10.Car[])__res;
  }


  public void setTestCars(org.openl.tablets.tutorial10.Car[] __var)
  {
   testCars_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField getCarPrice2009Test_Field;

  public org.openl.types.impl.DynamicObject[] getGetCarPrice2009Test()
  {
   Object __res = getCarPrice2009Test_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setGetCarPrice2009Test(org.openl.types.impl.DynamicObject[] __var)
  {
   getCarPrice2009Test_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testAddresses_Field;

  public org.openl.tablets.tutorial10.Address[] getTestAddresses()
  {
   Object __res = testAddresses_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial10.Address[])__res;
  }


  public void setTestAddresses(org.openl.tablets.tutorial10.Address[] __var)
  {
   testAddresses_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenMethod getDiscountPercentage_Method;
  public org.openl.meta.DoubleValue getDiscountPercentage(org.openl.tablets.tutorial10.Car car, int numberOfCars)  {
    Object[] __params = new Object[2];
    __params[0] = car;
    __params[1] = new Integer(numberOfCars);
    try
    {
    Object __myInstance = __instance;
    Object __res = getDiscountPercentage_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getPriceForOrder_Method;
  public org.openl.meta.DoubleValue getPriceForOrder(org.openl.tablets.tutorial10.Car car, int numberOfCars, org.openl.tablets.tutorial10.Address billingAddress)  {
    Object[] __params = new Object[3];
    __params[0] = car;
    __params[1] = new Integer(numberOfCars);
    __params[2] = billingAddress;
    try
    {
    Object __myInstance = __instance;
    Object __res = getPriceForOrder_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getPriceForOrder2009TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult getPriceForOrder2009TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = getPriceForOrder2009TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getCarPrice2009TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult getCarPrice2009TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = getCarPrice2009TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getCarPrice2010TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult getCarPrice2010TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = getCarPrice2010TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getCarPrice_Method;
  public org.openl.meta.DoubleValue getCarPrice(org.openl.tablets.tutorial10.Car car, org.openl.tablets.tutorial10.Address billingAddress)  {
    Object[] __params = new Object[2];
    __params[0] = car;
    __params[1] = billingAddress;
    try
    {
    Object __myInstance = __instance;
    Object __res = getCarPrice_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getPriceForOrder2010TestTestAll_Method;
  public org.openl.rules.testmethod.TestResult getPriceForOrder2010TestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = getPriceForOrder2010TestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod getDiscountPercentageTestTestAll_Method;
  public org.openl.rules.testmethod.TestResult getDiscountPercentageTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = getDiscountPercentageTestTestAll_Method.invoke(__myInstance, __params, __env.get());
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

    getPriceForOrder2010Test_Field = __class.getField("getPriceForOrder2010Test");
    getCarPrice2010Test_Field = __class.getField("getCarPrice2010Test");
    getDiscountPercentageTest_Field = __class.getField("getDiscountPercentageTest");
    getPriceForOrder2009Test_Field = __class.getField("getPriceForOrder2009Test");
    testCars_Field = __class.getField("testCars");
    getCarPrice2009Test_Field = __class.getField("getCarPrice2009Test");
    testAddresses_Field = __class.getField("testAddresses");
    this_Field = __class.getField("this");
    getDiscountPercentage_Method = __class.getMatchingMethod("getDiscountPercentage", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial10.Car.class),
      JavaOpenClass.getOpenClass(int.class)});
    getPriceForOrder_Method = __class.getMatchingMethod("getPriceForOrder", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial10.Car.class),
      JavaOpenClass.getOpenClass(int.class),
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial10.Address.class)});
    getPriceForOrder2009TestTestAll_Method = __class.getMatchingMethod("getPriceForOrder2009TestTestAll", new IOpenClass[] {
});
    getCarPrice2009TestTestAll_Method = __class.getMatchingMethod("getCarPrice2009TestTestAll", new IOpenClass[] {
});
    getCarPrice2010TestTestAll_Method = __class.getMatchingMethod("getCarPrice2010TestTestAll", new IOpenClass[] {
});
    getCarPrice_Method = __class.getMatchingMethod("getCarPrice", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial10.Car.class),
      JavaOpenClass.getOpenClass(org.openl.tablets.tutorial10.Address.class)});
    getPriceForOrder2010TestTestAll_Method = __class.getMatchingMethod("getPriceForOrder2010TestTestAll", new IOpenClass[] {
});
    getDiscountPercentageTestTestAll_Method = __class.getMatchingMethod("getDiscountPercentageTestTestAll", new IOpenClass[] {
});

    __initialized=true;
  }
}