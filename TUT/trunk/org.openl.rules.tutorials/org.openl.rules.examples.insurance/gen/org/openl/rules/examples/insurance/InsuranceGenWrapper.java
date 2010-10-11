/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.rules.examples.insurance;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.OpenClassHelper;
import java.util.Map;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.source.impl.FileSourceCodeModule;

public class InsuranceGenWrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/Insurance.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.rules.examples.insurance";

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

  public InsuranceGenWrapper(){
    this(false);
  }

  public InsuranceGenWrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public InsuranceGenWrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public InsuranceGenWrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public InsuranceGenWrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    __externalParams = params;
    __executionMode = executionMode;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField addresses_Field;

  public org.openl.generated.beans.Address[] getAddresses()
  {
   Object __res = addresses_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Address[])__res;
  }


  public void setAddresses(org.openl.generated.beans.Address[] __var)
  {
   addresses_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField vehicles_Field;

  public org.openl.generated.beans.InsurableVehicle[] getVehicles()
  {
   Object __res = vehicles_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.InsurableVehicle[])__res;
  }


  public void setVehicles(org.openl.generated.beans.InsurableVehicle[] __var)
  {
   vehicles_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField coverageTypes_Field;

  public org.openl.generated.beans.VehicleCoverageType[] getCoverageTypes()
  {
   Object __res = coverageTypes_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.VehicleCoverageType[])__res;
  }


  public void setCoverageTypes(org.openl.generated.beans.VehicleCoverageType[] __var)
  {
   coverageTypes_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField vehicleSymbols_Field;

  public org.openl.generated.beans.VehicleSymbol[] getVehicleSymbols()
  {
   Object __res = vehicleSymbols_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.VehicleSymbol[])__res;
  }


  public void setVehicleSymbols(org.openl.generated.beans.VehicleSymbol[] __var)
  {
   vehicleSymbols_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField limitsBI_Field;

  public org.openl.generated.beans.LimitsAndFactors[] getLimitsBI()
  {
   Object __res = limitsBI_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.LimitsAndFactors[])__res;
  }


  public void setLimitsBI(org.openl.generated.beans.LimitsAndFactors[] __var)
  {
   limitsBI_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField coverageBases_Field;

  public org.openl.generated.beans.CoverageBase[] getCoverageBases()
  {
   Object __res = coverageBases_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.CoverageBase[])__res;
  }


  public void setCoverageBases(org.openl.generated.beans.CoverageBase[] __var)
  {
   coverageBases_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField usages_Field;

  public org.openl.generated.beans.Usage[] getUsages()
  {
   Object __res = usages_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Usage[])__res;
  }


  public void setUsages(org.openl.generated.beans.Usage[] __var)
  {
   usages_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenField limitsPD_Field;

  public org.openl.generated.beans.LimitsAndFactors[] getLimitsPD()
  {
   Object __res = limitsPD_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.LimitsAndFactors[])__res;
  }


  public void setLimitsPD(org.openl.generated.beans.LimitsAndFactors[] __var)
  {
   limitsPD_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField drivers_Field;

  public org.openl.generated.beans.InsurableDriver[] getDrivers()
  {
   Object __res = drivers_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.InsurableDriver[])__res;
  }


  public void setDrivers(org.openl.generated.beans.InsurableDriver[] __var)
  {
   drivers_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField limitsMP_Field;

  public org.openl.generated.beans.LimitsAndFactors[] getLimitsMP()
  {
   Object __res = limitsMP_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.LimitsAndFactors[])__res;
  }


  public void setLimitsMP(org.openl.generated.beans.LimitsAndFactors[] __var)
  {
   limitsMP_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenMethod displayDiscount_Method;
  public void displayDiscount(org.openl.generated.beans.Discount d)  {
    Object[] __params = new Object[1];
    __params[0] = d;
    try
    {
    Object __myInstance = __instance;
    displayDiscount_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod validateVehicle_Method;
  public void validateVehicle(org.openl.generated.beans.InsurableVehicle vehicle, org.openl.generated.beans.InsurancePolicy policy, org.openl.generated.beans.PolicyPremiumCalculator pc)  {
    Object[] __params = new Object[3];
    __params[0] = vehicle;
    __params[1] = policy;
    __params[2] = pc;
    try
    {
    Object __myInstance = __instance;
    validateVehicle_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod displayRejections_Method;
  public void displayRejections(org.openl.generated.beans.PolicyPremiumCalculator pc)  {
    Object[] __params = new Object[1];
    __params[0] = pc;
    try
    {
    Object __myInstance = __instance;
    displayRejections_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod designateDriver_Method;
  public org.openl.generated.beans.InsurableDriver designateDriver(org.openl.generated.beans.InsurableVehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = designateDriver_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.generated.beans.InsurableDriver)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod defineVehicleDiscounts_Method;
  public void defineVehicleDiscounts(org.openl.generated.beans.InsurableVehicle vehicle, org.openl.generated.beans.InsurancePolicy policy, org.openl.generated.beans.VehiclePremiumCalculator calc)  {
    Object[] __params = new Object[3];
    __params[0] = vehicle;
    __params[1] = policy;
    __params[2] = calc;
    try
    {
    Object __myInstance = __instance;
    defineVehicleDiscounts_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod defineDriverDiscounts_Method;
  public void defineDriverDiscounts(org.openl.generated.beans.InsurableDriver driver, org.openl.generated.beans.InsurancePolicy policy, org.openl.generated.beans.VehiclePremiumCalculator calc)  {
    Object[] __params = new Object[3];
    __params[0] = driver;
    __params[1] = policy;
    __params[2] = calc;
    try
    {
    Object __myInstance = __instance;
    defineDriverDiscounts_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod validateDriver_Method;
  public void validateDriver(org.openl.generated.beans.InsurableDriver driver, org.openl.generated.beans.InsurancePolicy policy, org.openl.generated.beans.PolicyPremiumCalculator pc)  {
    Object[] __params = new Object[3];
    __params[0] = driver;
    __params[1] = policy;
    __params[2] = pc;
    try
    {
    Object __myInstance = __instance;
    validateDriver_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod validateInsurancePolicy_Method;
  public boolean validateInsurancePolicy(org.openl.generated.beans.InsurancePolicy policy, org.openl.generated.beans.PolicyPremiumCalculator pc)  {
    Object[] __params = new Object[2];
    __params[0] = policy;
    __params[1] = pc;
    try
    {
    Object __myInstance = __instance;
    Object __res = validateInsurancePolicy_Method.invoke(__myInstance, __params, __env.get());
   return ((Boolean)__res).booleanValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod main_Method;
  public void main(java.lang.String[] args)  {
    Object[] __params = new Object[1];
    __params[0] = args;
    try
    {
    Object __myInstance = __instance;
    main_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculatePremiums_Method;
  public double calculatePremiums(org.openl.generated.beans.InsurancePolicy policy)  {
    Object[] __params = new Object[1];
    __params[0] = policy;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculatePremiums_Method.invoke(__myInstance, __params, __env.get());
   return ((Double)__res).doubleValue();  }
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
    __params[3] = new Boolean(showInPolicy);
    try
    {
    Object __myInstance = __instance;
    addDiscount_Method.invoke(__myInstance, __params, __env.get());  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculateVehiclePremium_Method;
  public double calculateVehiclePremium(org.openl.generated.beans.VehiclePremiumCalculator calc)  {
    Object[] __params = new Object[1];
    __params[0] = calc;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculateVehiclePremium_Method.invoke(__myInstance, __params, __env.get());
   return ((Double)__res).doubleValue();  }
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
    FileSourceCodeModule source = new FileSourceCodeModule(__src, null);
    source.setParams(__externalParams);
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , source, __executionMode);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env.set(wrapper.getEnv());

    addresses_Field = __class.getField("addresses");
    vehicles_Field = __class.getField("vehicles");
    coverageTypes_Field = __class.getField("coverageTypes");
    vehicleSymbols_Field = __class.getField("vehicleSymbols");
    limitsBI_Field = __class.getField("limitsBI");
    coverageBases_Field = __class.getField("coverageBases");
    usages_Field = __class.getField("usages");
    this_Field = __class.getField("this");
    limitsPD_Field = __class.getField("limitsPD");
    drivers_Field = __class.getField("drivers");
    limitsMP_Field = __class.getField("limitsMP");
    displayDiscount_Method = __class.getMatchingMethod("displayDiscount", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Discount.class)});
    validateVehicle_Method = __class.getMatchingMethod("validateVehicle", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurableVehicle.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.PolicyPremiumCalculator.class)});
    displayRejections_Method = __class.getMatchingMethod("displayRejections", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.PolicyPremiumCalculator.class)});
    designateDriver_Method = __class.getMatchingMethod("designateDriver", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurableVehicle.class)});
    defineVehicleDiscounts_Method = __class.getMatchingMethod("defineVehicleDiscounts", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurableVehicle.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.VehiclePremiumCalculator.class)});
    defineDriverDiscounts_Method = __class.getMatchingMethod("defineDriverDiscounts", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurableDriver.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.VehiclePremiumCalculator.class)});
    validateDriver_Method = __class.getMatchingMethod("validateDriver", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurableDriver.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.PolicyPremiumCalculator.class)});
    validateInsurancePolicy_Method = __class.getMatchingMethod("validateInsurancePolicy", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class),
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.PolicyPremiumCalculator.class)});
    main_Method = __class.getMatchingMethod("main", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String[].class)});
    calculatePremiums_Method = __class.getMatchingMethod("calculatePremiums", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.InsurancePolicy.class)});
    addDiscount_Method = __class.getMatchingMethod("addDiscount", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.util.Vector.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, double.class),
      OpenClassHelper.getOpenClass(__class, boolean.class)});
    calculateVehiclePremium_Method = __class.getMatchingMethod("calculateVehiclePremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.VehiclePremiumCalculator.class)});

    __initialized=true;
  }
}