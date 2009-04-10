/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package template;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class TemplateJavaWrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

  public static org.openl.vm.IRuntimeEnv __env;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/TemplateRules.xls";

  public static String __userHome = ".";

  public TemplateJavaWrapper(){
    this(false);
  }

  public TemplateJavaWrapper(boolean ignoreErrors){
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



  static org.openl.types.IOpenMethod hello1_Method;
  public void hello1(int hour)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(hour);
    try
    {
    Object __myInstance = __instance;
    hello1_Method.invoke(__myInstance, __params, __env);  }
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
    hello1_Method = __class.getMatchingMethod("hello1", new IOpenClass[] {
      JavaOpenClass.getOpenClass(int.class)});

    __initialized=true;
  }
}