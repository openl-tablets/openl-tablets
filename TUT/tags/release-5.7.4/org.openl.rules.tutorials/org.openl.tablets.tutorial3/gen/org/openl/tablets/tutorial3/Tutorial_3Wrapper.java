/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial3;

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

public class Tutorial_3Wrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static IDependencyManager __dependencyManager;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/Tutorial_3.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial3";

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

  public Tutorial_3Wrapper(){
    this(false);
  }

  public Tutorial_3Wrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public Tutorial_3Wrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public Tutorial_3Wrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public Tutorial_3Wrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    this(ignoreErrors, executionMode, params, null);
  }

  public Tutorial_3Wrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params, IDependencyManager dependencyManager){
    __externalParams = params;
    __executionMode = executionMode;
    __dependencyManager = dependencyManager;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField addresses31_Field;

  public org.openl.tablets.tutorial3.Address[] getAddresses31()
  {
   Object __res = addresses31_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial3.Address[])__res;
  }


  public void setAddresses31(org.openl.tablets.tutorial3.Address[] __var)
  {
   addresses31_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField states_Field;

  public org.openl.tablets.tutorial3.USState[] getStates()
  {
   Object __res = states_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial3.USState[])__res;
  }


  public void setStates(org.openl.tablets.tutorial3.USState[] __var)
  {
   states_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField addresses3_Field;

  public org.openl.tablets.tutorial3.Address[] getAddresses3()
  {
   Object __res = addresses3_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial3.Address[])__res;
  }


  public void setAddresses3(org.openl.tablets.tutorial3.Address[] __var)
  {
   addresses3_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenMethod test21_Method;
  public java.lang.String test21()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test21_Method.invoke(__myInstance, __params, __env.get());
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
    Object __res = ampmTo24_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
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
    Object __res = region22_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
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


  static org.openl.types.IOpenMethod hr24ToAmpm_Method;
  public java.lang.String hr24ToAmpm(int hr24)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(hr24);
    try
    {
    Object __myInstance = __instance;
    Object __res = hr24ToAmpm_Method.invoke(__myInstance, __params, __env.get());
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
    Object __res = region21_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
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

    addresses31_Field = __class.getField("addresses31");
    states_Field = __class.getField("states");
    addresses3_Field = __class.getField("addresses3");
    this_Field = __class.getField("this");
    test21_Method = __class.getMatchingMethod("test21", new IOpenClass[] {
});
    ampmTo24_Method = __class.getMatchingMethod("ampmTo24", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    region22_Method = __class.getMatchingMethod("region22", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    region_Method = __class.getMatchingMethod("region", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    hr24ToAmpm_Method = __class.getMatchingMethod("hr24ToAmpm", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class)});
    region21_Method = __class.getMatchingMethod("region21", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});

    __initialized=true;
  }
}