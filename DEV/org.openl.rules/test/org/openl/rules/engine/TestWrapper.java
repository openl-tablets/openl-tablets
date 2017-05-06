/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.rules.engine;

import org.junit.Ignore;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

@Ignore("Auxiliary class")
public class TestWrapper implements org.openl.rules.context.IRulesRuntimeContextProvider
{
  Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

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

  static synchronized protected void __init()
  {
    if (__initialized)
      return;

    RulesEngineFactory engineFactory = new RulesEngineFactory("./test/rules/engine/RulesContextTest.xls");
    __compiledClass = engineFactory.getCompiledOpenClass();
    __class = __compiledClass.getOpenClassWithErrors();

    this_Field = __class.getField("this");
    driverRiskScoreOverloadTest_Method = __class.getMatchingMethod("driverRiskScoreOverloadTest", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});
    driverRiskScoreNoOverloadTest_Method = __class.getMatchingMethod("driverRiskScoreNoOverloadTest", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String.class)});

    __initialized=true;
  }
}