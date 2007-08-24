/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial1;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class Tutorial_1Wrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.vm.IRuntimeEnv __env;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_1.xls";

  public static String __userHome = ".";

  public Tutorial_1Wrapper(){
    this(false);
  }

  public Tutorial_1Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env);
  }



  static org.openl.types.IOpenField loanRequests_Field;

  public org.openl.types.impl.DynamicObject[] getLoanRequests()
  {
   Object __res = loanRequests_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setLoanRequests(org.openl.types.impl.DynamicObject[] __var)
  {
   loanRequests_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField customers_Field;

  public org.openl.types.impl.DynamicObject[] getCustomers()
  {
   Object __res = customers_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setCustomers(org.openl.types.impl.DynamicObject[] __var)
  {
   customers_Field.set(__instance, __var, __env);
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



  static org.openl.types.IOpenMethod main_Method;
  public static void main(java.lang.String[] args)  {
    Object[] __params = new Object[1];
    __params[0] = args;
    try
    {
    Object __myInstance = new Tutorial_1Wrapper().__instance;
    main_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod TopLevelRules_Method;
  public void TopLevelRules(org.openl.types.impl.DynamicObject loan)  {
    Object[] __params = new Object[1];
    __params[0] = loan;
    try
    {
    Object __myInstance = __instance;
    TopLevelRules_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod DebtResearchRules_Method;
  public void DebtResearchRules(org.openl.types.impl.DynamicObject loan, org.openl.types.impl.DynamicObject c)  {
    Object[] __params = new Object[2];
    __params[0] = loan;
    __params[1] = c;
    try
    {
    Object __myInstance = __instance;
    DebtResearchRules_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ValidateIncomeRules_Method;
  public void ValidateIncomeRules(org.openl.types.impl.DynamicObject loan, org.openl.types.impl.DynamicObject customer)  {
    Object[] __params = new Object[2];
    __params[0] = loan;
    __params[1] = customer;
    try
    {
    Object __myInstance = __instance;
    ValidateIncomeRules_Method.invoke(__myInstance, __params, __env);  }
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
    __env = wrapper.getEnv();

    loanRequests_Field = __class.getField("loanRequests");
    customers_Field = __class.getField("customers");
    this_Field = __class.getField("this");
    main_Method = __class.getMatchingMethod("main", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String[].class)});
    TopLevelRules_Method = __class.getMatchingMethod("TopLevelRules", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.types.impl.DynamicObject.class)});
    DebtResearchRules_Method = __class.getMatchingMethod("DebtResearchRules", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.types.impl.DynamicObject.class),
      JavaOpenClass.getOpenClass(org.openl.types.impl.DynamicObject.class)});
    ValidateIncomeRules_Method = __class.getMatchingMethod("ValidateIncomeRules", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.types.impl.DynamicObject.class),
      JavaOpenClass.getOpenClass(org.openl.types.impl.DynamicObject.class)});

    __initialized=true;
  }
}