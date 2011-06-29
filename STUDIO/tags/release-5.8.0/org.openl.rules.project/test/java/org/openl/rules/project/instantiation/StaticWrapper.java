/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.rules.project.instantiation;

import java.util.Map;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.java.OpenClassHelper;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.impl.OpenClassJavaWrapper;

public class StaticWrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{

  public static java.lang.String __src = "test/resources/excel/Rules.xls";

  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static IDependencyManager __dependencyManager;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial1";

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

  public StaticWrapper(){
    this(false);
  }

  public StaticWrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public StaticWrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public StaticWrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public StaticWrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    this(ignoreErrors, executionMode, params, null);
  }

  public StaticWrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params, IDependencyManager dependencyManager){
    __externalParams = params;
    __executionMode = executionMode;
    __dependencyManager = dependencyManager;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
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



  static org.openl.types.IOpenMethod hello1_Method;
  public java.lang.String hello1(int hour)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(hour);
    try
    {
    Object __myInstance = __instance;
    Object __res = hello1_Method.invoke(__myInstance, __params, __env.get());
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

    this_Field = __class.getField("this");
    hello1_Method = __class.getMatchingMethod("hello1", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class)});

    __initialized=true;
  }
}