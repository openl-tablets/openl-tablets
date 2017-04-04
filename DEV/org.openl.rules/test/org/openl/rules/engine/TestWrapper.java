/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.rules.engine;

import org.junit.Ignore;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

@Ignore("Auxiliary class")
public class TestWrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider
{
  Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "test/rules/engine/RulesContextTest.xls";

  public static String __srcModuleClass = null;

  public static String __folder = "rules";

  public static String __project = "org.openl.rules.engine";

  public static String __userHome = ".";

  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
    @Override
    protected org.openl.vm.IRuntimeEnv initialValue() {
      org.openl.vm.IRuntimeEnv environment = new SimpleRulesVM().getRuntimeEnv();
      environment.setContext(RulesRuntimeContextFactory.buildRulesRuntimeContext());
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

  public TestWrapper(){
    this(false);
  }

  public TestWrapper(boolean ignoreErrors){
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



  static org.openl.types.IOpenMethod driverRiskScoreOverloadTest_Method;
  public org.openl.meta.DoubleValue driverRiskScoreOverloadTest(java.lang.String driverRisk)  {
    Object[] __params = new Object[1];
    __params[0] = driverRisk;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRiskScoreOverloadTest_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverRiskScoreNoOverloadTest_Method;
  public org.openl.meta.DoubleValue driverRiskScoreNoOverloadTest(java.lang.String driverRisk)  {
    Object[] __params = new Object[1];
    __params[0] = driverRisk;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRiskScoreNoOverloadTest_Method.invoke(__myInstance, __params, __env.get());
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

    IUserContext ucxt = UserContext.getCurrentContextOrCreateNew(Thread.currentThread().getContextClassLoader(), __userHome);
    __compiledClass = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src, null);
    __class = __compiledClass.getOpenClassWithErrors();

    this_Field = __class.getField("this");
    driverRiskScoreOverloadTest_Method = __class.getMatchingMethod("driverRiskScoreOverloadTest", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});
    driverRiskScoreNoOverloadTest_Method = __class.getMatchingMethod("driverRiskScoreNoOverloadTest", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});

    __initialized=true;
  }
}