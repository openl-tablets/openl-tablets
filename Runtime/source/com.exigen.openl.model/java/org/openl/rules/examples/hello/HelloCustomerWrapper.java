/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.rules.examples.hello;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class HelloCustomerWrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.vm.IRuntimeEnv __env;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "HelloCustomer.xls";

  public static String __userHome = ".";

  public HelloCustomerWrapper(){
    this(false);
  }

  public HelloCustomerWrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env);
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



  static org.openl.types.IOpenMethod defineGreeting_Method;
  public void defineGreeting(int hour, org.openl.rules.examples.hello.Response response)  {
    Object[] __params = new Object[2];
    __params[0] = new Integer(hour);
    __params[1] = response;
    try
    {
    Object __myInstance = __instance;
    defineGreeting_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod helloCustomer_Method;
  public void helloCustomer(org.openl.rules.examples.hello.Customer customer, org.openl.rules.examples.hello.Response response)  {
    Object[] __params = new Object[2];
    __params[0] = customer;
    __params[1] = response;
    try
    {
    Object __myInstance = __instance;
    helloCustomer_Method.invoke(__myInstance, __params, __env);  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod defineSalutation_Method;
  public void defineSalutation(org.openl.rules.examples.hello.Customer customer, org.openl.rules.examples.hello.Response response)  {
    Object[] __params = new Object[2];
    __params[0] = customer;
    __params[1] = response;
    try
    {
    Object __myInstance = __instance;
    defineSalutation_Method.invoke(__myInstance, __params, __env);  }
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

    this_Field = __class.getField("this");
    defineGreeting_Method = __class.getMatchingMethod("defineGreeting", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class),
      JavaOpenClass.getOpenClass(org.openl.rules.examples.hello.Response.class)});
    helloCustomer_Method = __class.getMatchingMethod("helloCustomer", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.rules.examples.hello.Customer.class),
      JavaOpenClass.getOpenClass(org.openl.rules.examples.hello.Response.class)});
    defineSalutation_Method = __class.getMatchingMethod("defineSalutation", new IOpenClass[] {
      JavaOpenClass.getOpenClass(org.openl.rules.examples.hello.Customer.class),
      JavaOpenClass.getOpenClass(org.openl.rules.examples.hello.Response.class)});

    __initialized=true;
  }
}