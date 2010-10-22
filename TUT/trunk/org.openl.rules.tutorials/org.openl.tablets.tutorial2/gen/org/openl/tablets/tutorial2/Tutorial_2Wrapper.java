/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial2;

import java.util.Map;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.source.IOpenSourceCodeModule;

public class Tutorial_2Wrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/Tutorial_2.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial2";

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

  public Tutorial_2Wrapper(){
    this(false);
  }

  public Tutorial_2Wrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public Tutorial_2Wrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public Tutorial_2Wrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public Tutorial_2Wrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    __externalParams = params;
    __executionMode = executionMode;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField pp11_Field;

  public org.openl.generated.beans.Person1[] getPp11()
  {
   Object __res = pp11_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Person1[])__res;
  }


  public void setPp11(org.openl.generated.beans.Person1[] __var)
  {
   pp11_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField phrases21_Field;

  public java.lang.String[] getPhrases21()
  {
   Object __res = phrases21_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setPhrases21(java.lang.String[] __var)
  {
   phrases21_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField numbers22_Field;

  public int[] getNumbers22()
  {
   Object __res = numbers22_Field.get(__instance, __env.get());
   return (int[])__res;
  }


  public void setNumbers22(int[] __var)
  {
   numbers22_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField pp1_Field;

  public org.openl.generated.beans.Person1[] getPp1()
  {
   Object __res = pp1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Person1[])__res;
  }


  public void setPp1(org.openl.generated.beans.Person1[] __var)
  {
   pp1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField customers3_Field;

  public org.openl.tablets.tutorial2.step3.Customer2_3[] getCustomers3()
  {
   Object __res = customers3_Field.get(__instance, __env.get());
   return (org.openl.tablets.tutorial2.step3.Customer2_3[])__res;
  }


  public void setCustomers3(org.openl.tablets.tutorial2.step3.Customer2_3[] __var)
  {
   customers3_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenField ranges23_Field;

  public org.openl.rules.helpers.IntRange[] getRanges23()
  {
   Object __res = ranges23_Field.get(__instance, __env.get());
   return (org.openl.rules.helpers.IntRange[])__res;
  }


  public void setRanges23(org.openl.rules.helpers.IntRange[] __var)
  {
   ranges23_Field.set(__instance, __var, __env.get());
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
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , source, __executionMode);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env.set(wrapper.getEnv());

    pp11_Field = __class.getField("pp11");
    phrases21_Field = __class.getField("phrases21");
    numbers22_Field = __class.getField("numbers22");
    pp1_Field = __class.getField("pp1");
    customers3_Field = __class.getField("customers3");
    this_Field = __class.getField("this");
    ranges23_Field = __class.getField("ranges23");

    __initialized=true;
  }
}