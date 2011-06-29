/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it
 */

package org.openl.tablets.tutorial2;

import org.openl.types.IOpenClass;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
public class Tutorial_2Wrapper implements org.openl.main.OpenLWrapper
{
  Object __instance;

    public org.openl.vm.IRuntimeEnv __env = new org.openl.vm.SimpleVM().getRuntimeEnv();

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  public static String __openlName = "org.openl.xls";

  public static String __src = "rules/Tutorial_2.xls";

  public static String __folder = "rules";

  public static String __project = "org.openl.tablets.tutorial2";

  public static String __userHome = ".";

  public Tutorial_2Wrapper(){
    this(false);
  }

  public Tutorial_2Wrapper(boolean ignoreErrors){
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env);
  }



  static org.openl.types.IOpenField pp11_Field;

  public org.openl.types.impl.DynamicObject[] getPp11()
  {
   Object __res = pp11_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setPp11(org.openl.types.impl.DynamicObject[] __var)
  {
   pp11_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField numbers22_Field;

  public int[] getNumbers22()
  {
   Object __res = numbers22_Field.get(__instance, __env);
   return (int[])__res;
  }


  public void setNumbers22(int[] __var)
  {
   numbers22_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField phrases21_Field;

  public java.lang.String[] getPhrases21()
  {
   Object __res = phrases21_Field.get(__instance, __env);
   return (java.lang.String[])__res;
  }


  public void setPhrases21(java.lang.String[] __var)
  {
   phrases21_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField pp1_Field;

  public org.openl.types.impl.DynamicObject[] getPp1()
  {
   Object __res = pp1_Field.get(__instance, __env);
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setPp1(org.openl.types.impl.DynamicObject[] __var)
  {
   pp1_Field.set(__instance, __var, __env);
  }



  static org.openl.types.IOpenField customers3_Field;

  public org.openl.tablets.tutorial2.step3.Customer2_3[] getCustomers3()
  {
   Object __res = customers3_Field.get(__instance, __env);
   return (org.openl.tablets.tutorial2.step3.Customer2_3[])__res;
  }


  public void setCustomers3(org.openl.tablets.tutorial2.step3.Customer2_3[] __var)
  {
   customers3_Field.set(__instance, __var, __env);
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



  static org.openl.types.IOpenField ranges23_Field;

  public org.openl.rules.helpers.IntRange[] getRanges23()
  {
   Object __res = ranges23_Field.get(__instance, __env);
   return (org.openl.rules.helpers.IntRange[])__res;
  }


  public void setRanges23(org.openl.rules.helpers.IntRange[] __var)
  {
   ranges23_Field.set(__instance, __var, __env);
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
   // __env = wrapper.getEnv();

    pp11_Field = __class.getField("pp11");
    numbers22_Field = __class.getField("numbers22");
    phrases21_Field = __class.getField("phrases21");
    pp1_Field = __class.getField("pp1");
    customers3_Field = __class.getField("customers3");
    this_Field = __class.getField("this");
    ranges23_Field = __class.getField("ranges23");

    __initialized=true;
  }
}