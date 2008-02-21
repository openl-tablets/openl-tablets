package org.openl.rules.examples.insurance;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.conf.UserContext;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.impl.OpenClassJavaWrapper;
public class InsuranceGenWrapper
{
  Object __instance;

  public static org.openl.vm.IRuntimeEnv __env;

  public static org.openl.types.IOpenClass __class;

  public static String __openlName = "org.openl.xls";

  public static String __src = "Insurance.xls";

  public static String __userHome = ".";

  public InsuranceGenWrapper()
  {
    __init();
    __instance = __class.newInstance(__env);
  }


  static org.openl.types.IOpenMethod main_Method;
  public static void main(java.lang.String[] args)  {
    Object[] __params = new Object[1];
    __params[0] = args;    try
    {
    Object __myInstance = new InsuranceGenWrapper().__instance;
    main_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod addDiscount_Method;
  public void addDiscount(java.util.Vector discounts, java.lang.String type, double value, boolean showInPolicy)  {
    Object[] __params = new Object[4];
    __params[0] = discounts;
    __params[1] = type;
    __params[2] = new Double(value);
    __params[3] = new Boolean(showInPolicy);    try
    {
    Object __myInstance = __instance;
    addDiscount_Method.invoke(__myInstance, __params, __env);  }
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

    UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), __userHome);
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src);
    __class = wrapper.getOpenClass();
    __env = wrapper.getEnv();

    main_Method = __class.getMatchingMethod("main", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.lang.String[].class)});
    addDiscount_Method = __class.getMatchingMethod("addDiscount", new IOpenClass[] {
      JavaOpenClass.getOpenClass(java.util.Vector.class),
      JavaOpenClass.getOpenClass(java.lang.String.class),
      JavaOpenClass.getOpenClass(double.class),
      JavaOpenClass.getOpenClass(boolean.class)});

    __initialized=true;
  }
}