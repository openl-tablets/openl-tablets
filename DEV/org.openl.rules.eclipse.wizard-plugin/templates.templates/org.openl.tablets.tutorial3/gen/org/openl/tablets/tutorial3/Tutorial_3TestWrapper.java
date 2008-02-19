/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial3;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class Tutorial_3TestWrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

    public org.openl.vm.IRuntimeEnv __env = new org.openl.vm.SimpleVM().getRuntimeEnv();

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_3_Tests.xls";

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial3";

  public static String __userHome = ".";

  public Tutorial_3TestWrapper(){
    this(false);
  }

  public Tutorial_3TestWrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env);
  }



  static org.openl.types.IOpenField states_Field;

  public org.openl.tablets.tutorial3.USState[] getStates()
  {
   Object __res = states_Field.get(__instance, __env);
   return (org.openl.tablets.tutorial3.USState[])__res;
  }


  public void setStates(org.openl.tablets.tutorial3.USState[] __var)
  {
   states_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField addresses31_Field;

  public org.openl.tablets.tutorial3.Address[] getAddresses31()
  {
   Object __res = addresses31_Field.get(__instance, __env);
   return (org.openl.tablets.tutorial3.Address[])__res;
  }


  public void setAddresses31(org.openl.tablets.tutorial3.Address[] __var)
  {
   addresses31_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField ampmTo24Test_Field;

  public org.openl.types.impl.DynamicObject[] getAmpmTo24Test()
  {
   Object __res = ampmTo24Test_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setAmpmTo24Test(org.openl.types.impl.DynamicObject[] __var)
  {
   ampmTo24Test_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField addresses3_Field;

  public org.openl.tablets.tutorial3.Address[] getAddresses3()
  {
   Object __res = addresses3_Field.get(__instance, __env);
   return (org.openl.tablets.tutorial3.Address[])__res;
  }


  public void setAddresses3(org.openl.tablets.tutorial3.Address[] __var)
  {
   addresses3_Field.set(__instance, __var, __env);
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



  static org.openl.types.IOpenMethod region_Method;
  public java.lang.String region(java.lang.String state)  {
    Object[] __params = new Object[1];
    __params[0] = state;
    try
    {
    Object __myInstance = __instance;
    Object __res = region_Method.invoke(__myInstance, __params, __env);
   return (java.lang.String)__res;  }
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
    Object __res = ampmTo24TestTestAll_Method.invoke(__myInstance, __params, __env);
   return (org.openl.rules.testmethod.TestResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod test21_Method;
  public java.lang.String test21()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test21_Method.invoke(__myInstance, __params, __env);
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod region22_Method;
  public java.lang.String region22(java.lang.String state)  {
    Object[] __params = new Object[1];
    __params[0] = state;
    try
    {
    Object __myInstance = __instance;
    Object __res = region22_Method.invoke(__myInstance, __params, __env);
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod region21_Method;
  public java.lang.String region21(java.lang.String state)  {
    Object[] __params = new Object[1];
    __params[0] = state;
    try
    {
    Object __myInstance = __instance;
    Object __res = region21_Method.invoke(__myInstance, __params, __env);
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod hr24ToAmpm_Method;
  public java.lang.String hr24ToAmpm(int hr24)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(hr24);
    try
    {
    Object __myInstance = __instance;
    Object __res = hr24ToAmpm_Method.invoke(__myInstance, __params, __env);
   return (java.lang.String)__res;  }
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
    Object __res = ampmTo24_Method.invoke(__myInstance, __params, __env);
   return ((Integer)__res).intValue();  }
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

    states_Field = __class.getField("states");
    addresses31_Field = __class.getField("addresses31");
    ampmTo24Test_Field = __class.getField("ampmTo24Test");
    addresses3_Field = __class.getField("addresses3");
    this_Field = __class.getField("this");
    region_Method = __class.getMatchingMethod("region", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});
    ampmTo24TestTestAll_Method = __class.getMatchingMethod("ampmTo24TestTestAll", new IOpenClass[] {
});
    test21_Method = __class.getMatchingMethod("test21", new IOpenClass[] {
});
    region22_Method = __class.getMatchingMethod("region22", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});
    region21_Method = __class.getMatchingMethod("region21", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});
    hr24ToAmpm_Method = __class.getMatchingMethod("hr24ToAmpm", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class)});
    ampmTo24_Method = __class.getMatchingMethod("ampmTo24", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class),
      JavaOpenClass.getOpenClass(java.lang.String.class)});

    __initialized=true;
  }
}