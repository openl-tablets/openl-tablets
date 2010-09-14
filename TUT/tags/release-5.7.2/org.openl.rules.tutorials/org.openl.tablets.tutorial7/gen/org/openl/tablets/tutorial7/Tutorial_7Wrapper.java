/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial7;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.OpenClassHelper;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;

public class Tutorial_7Wrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/Tutorial_7.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial7";

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

  public Tutorial_7Wrapper(){
    this(false);
  }

  public Tutorial_7Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
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



  static org.openl.types.IOpenField test2_Field;

  public org.openl.types.impl.DynamicObject[] getTest2()
  {
   Object __res = test2_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setTest2(org.openl.types.impl.DynamicObject[] __var)
  {
   test2_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField test3_Field;

  public org.openl.types.impl.DynamicObject[] getTest3()
  {
   Object __res = test3_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setTest3(org.openl.types.impl.DynamicObject[] __var)
  {
   test3_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenMethod scoreIssueImportance_Method;
  public java.lang.String scoreIssueImportance(org.openl.tablets.tutorial7.Issue issue)  {
    Object[] __params = new Object[1];
    __params[0] = issue;
    try
    {
    Object __myInstance = __instance;
    Object __res = scoreIssueImportance_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod test1TestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults test1TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test1TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod scoreIssue_Method;
  public int scoreIssue(org.openl.tablets.tutorial7.Issue issue)  {
    Object[] __params = new Object[1];
    __params[0] = issue;
    try
    {
    Object __myInstance = __instance;
    Object __res = scoreIssue_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod test2TestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults test2TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test2TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod needApprovalOf_Method;
  public java.lang.String needApprovalOf(org.openl.tablets.tutorial7.Expense expense)  {
    Object[] __params = new Object[1];
    __params[0] = expense;
    try
    {
    Object __myInstance = __instance;
    Object __res = needApprovalOf_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod test3TestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults test3TestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = test3TestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
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

    test1_Field = __class.getField("test1");
    this_Field = __class.getField("this");
    test2_Field = __class.getField("test2");
    test3_Field = __class.getField("test3");
    scoreIssueImportance_Method = __class.getMatchingMethod("scoreIssueImportance", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.tablets.tutorial7.Issue.class)});
    test1TestAll_Method = __class.getMatchingMethod("test1TestAll", new IOpenClass[] {
});
    scoreIssue_Method = __class.getMatchingMethod("scoreIssue", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.tablets.tutorial7.Issue.class)});
    test2TestAll_Method = __class.getMatchingMethod("test2TestAll", new IOpenClass[] {
});
    needApprovalOf_Method = __class.getMatchingMethod("needApprovalOf", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.tablets.tutorial7.Expense.class)});
    test3TestAll_Method = __class.getMatchingMethod("test3TestAll", new IOpenClass[] {
});

    __initialized=true;
  }
}