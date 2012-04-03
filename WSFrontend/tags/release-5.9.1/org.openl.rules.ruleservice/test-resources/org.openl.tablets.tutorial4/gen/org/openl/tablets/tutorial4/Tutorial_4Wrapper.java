/*
 * This class has been generated. Do not change it, if you need to modify functionality - subclass it 
*/

package org.openl.tablets.tutorial4;

import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.types.java.OpenClassHelper;
import java.util.Map;
import org.openl.types.IOpenClass;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.dependency.IDependencyManager;

public class Tutorial_4Wrapper implements org.openl.main.OpenLWrapper,org.openl.rules.context.IRulesRuntimeContextProvider,org.openl.rules.context.IRulesRuntimeContextConsumer
{
  java.lang.Object __instance;

  public static org.openl.types.IOpenClass __class;

  public static org.openl.CompiledOpenClass __compiledClass;

  private static Map<String, Object> __externalParams;
  private static IDependencyManager __dependencyManager;
  private static boolean __executionMode;
  public static java.lang.String __openlName = "org.openl.xls";

  public static java.lang.String __src = "rules/main/Tutorial_4.xls";

  public static java.lang.String __srcModuleClass = null;

  public static java.lang.String __folder = "rules";

  public static java.lang.String __project = "org.openl.tablets.tutorial4";

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

  public Tutorial_4Wrapper(){
    this(false);
  }

  public Tutorial_4Wrapper(boolean ignoreErrors){
    this(ignoreErrors, false);
  }

  public Tutorial_4Wrapper(boolean ignoreErrors, boolean executionMode){
    this(ignoreErrors, executionMode, null);
  }

  public Tutorial_4Wrapper(Map<String, Object> params){
    this(false, false, params);
  }

  public Tutorial_4Wrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params){
    this(ignoreErrors, executionMode, params, null);
  }

  public Tutorial_4Wrapper(boolean ignoreErrors, boolean executionMode, Map<String, Object> params, IDependencyManager dependencyManager){
    __externalParams = params;
    __executionMode = executionMode;
    __dependencyManager = dependencyManager;
    __init();
    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();
    __instance = __class.newInstance(__env.get());
  }



  static org.openl.types.IOpenField driverRisk_Field;

  public java.lang.String[] getDriverRisk()
  {
   Object __res = driverRisk_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setDriverRisk(java.lang.String[] __var)
  {
   driverRisk_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverEligibilityTest_Field;

  public org.openl.types.impl.DynamicObject[] getDriverEligibilityTest()
  {
   Object __res = driverEligibilityTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDriverEligibilityTest(org.openl.types.impl.DynamicObject[] __var)
  {
   driverEligibilityTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField maritalStatus_Field;

  public java.lang.String[] getMaritalStatus()
  {
   Object __res = maritalStatus_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setMaritalStatus(java.lang.String[] __var)
  {
   maritalStatus_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField theft_rating_Field;

  public java.lang.String[] getTheft_rating()
  {
   Object __res = theft_rating_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setTheft_rating(java.lang.String[] __var)
  {
   theft_rating_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField catVehiclePremium_Field;

  public org.openl.rules.table.properties.TableProperties getCatVehiclePremium()
  {
   Object __res = catVehiclePremium_Field.get(__instance, __env.get());
   return (org.openl.rules.table.properties.TableProperties)__res;
  }


  public void setCatVehiclePremium(org.openl.rules.table.properties.TableProperties __var)
  {
   catVehiclePremium_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField car_type_Field;

  public java.lang.String[] getCar_type()
  {
   Object __res = car_type_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setCar_type(java.lang.String[] __var)
  {
   car_type_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField vehicleInjuryRatingTest_Field;

  public org.openl.types.impl.DynamicObject[] getVehicleInjuryRatingTest()
  {
   Object __res = vehicleInjuryRatingTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setVehicleInjuryRatingTest(org.openl.types.impl.DynamicObject[] __var)
  {
   vehicleInjuryRatingTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField policyProfile2_Field;

  public org.openl.generated.beans.Policy[] getPolicyProfile2()
  {
   Object __res = policyProfile2_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setPolicyProfile2(org.openl.generated.beans.Policy[] __var)
  {
   policyProfile2_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField policyProfile1_Field;

  public org.openl.generated.beans.Policy[] getPolicyProfile1()
  {
   Object __res = policyProfile1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setPolicyProfile1(org.openl.generated.beans.Policy[] __var)
  {
   policyProfile1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField airbag_type_Field;

  public java.lang.String[] getAirbag_type()
  {
   Object __res = airbag_type_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setAirbag_type(java.lang.String[] __var)
  {
   airbag_type_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverRiskTest_Field;

  public org.openl.types.impl.DynamicObject[] getDriverRiskTest()
  {
   Object __res = driverRiskTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDriverRiskTest(org.openl.types.impl.DynamicObject[] __var)
  {
   driverRiskTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField policyProfile4_Field;

  public org.openl.generated.beans.Policy[] getPolicyProfile4()
  {
   Object __res = policyProfile4_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setPolicyProfile4(org.openl.generated.beans.Policy[] __var)
  {
   policyProfile4_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField injury_rating_Field;

  public java.lang.String[] getInjury_rating()
  {
   Object __res = injury_rating_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setInjury_rating(java.lang.String[] __var)
  {
   injury_rating_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField policyProfile3_Field;

  public org.openl.generated.beans.Policy[] getPolicyProfile3()
  {
   Object __res = policyProfile3_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setPolicyProfile3(org.openl.generated.beans.Policy[] __var)
  {
   policyProfile3_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField gender_Field;

  public java.lang.String[] getGender()
  {
   Object __res = gender_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setGender(java.lang.String[] __var)
  {
   gender_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverProfiles1_Field;

  public org.openl.generated.beans.Driver[] getDriverProfiles1()
  {
   Object __res = driverProfiles1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Driver[])__res;
  }


  public void setDriverProfiles1(org.openl.generated.beans.Driver[] __var)
  {
   driverProfiles1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testVehicles1_Field;

  public org.openl.generated.beans.Vehicle[] getTestVehicles1()
  {
   Object __res = testVehicles1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Vehicle[])__res;
  }


  public void setTestVehicles1(org.openl.generated.beans.Vehicle[] __var)
  {
   testVehicles1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testDrivers1_Field;

  public org.openl.generated.beans.Driver[] getTestDrivers1()
  {
   Object __res = testDrivers1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Driver[])__res;
  }


  public void setTestDrivers1(org.openl.generated.beans.Driver[] __var)
  {
   testDrivers1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverProfiles2_Field;

  public org.openl.generated.beans.Driver[] getDriverProfiles2()
  {
   Object __res = driverProfiles2_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Driver[])__res;
  }


  public void setDriverProfiles2(org.openl.generated.beans.Driver[] __var)
  {
   driverProfiles2_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField moduleProp_Field;

  public org.openl.rules.table.properties.TableProperties getModuleProp()
  {
   Object __res = moduleProp_Field.get(__instance, __env.get());
   return (org.openl.rules.table.properties.TableProperties)__res;
  }


  public void setModuleProp(org.openl.rules.table.properties.TableProperties __var)
  {
   moduleProp_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverProfiles3_Field;

  public org.openl.generated.beans.Driver[] getDriverProfiles3()
  {
   Object __res = driverProfiles3_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Driver[])__res;
  }


  public void setDriverProfiles3(org.openl.generated.beans.Driver[] __var)
  {
   driverProfiles3_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testPolicy2_Field;

  public org.openl.generated.beans.Policy[] getTestPolicy2()
  {
   Object __res = testPolicy2_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setTestPolicy2(org.openl.generated.beans.Policy[] __var)
  {
   testPolicy2_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField vehicleTheftRatingTest_Field;

  public org.openl.types.impl.DynamicObject[] getVehicleTheftRatingTest()
  {
   Object __res = vehicleTheftRatingTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setVehicleTheftRatingTest(org.openl.types.impl.DynamicObject[] __var)
  {
   vehicleTheftRatingTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField testPolicy1_Field;

  public org.openl.generated.beans.Policy[] getTestPolicy1()
  {
   Object __res = testPolicy1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Policy[])__res;
  }


  public void setTestPolicy1(org.openl.generated.beans.Policy[] __var)
  {
   testPolicy1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField autoProfiles2_Field;

  public org.openl.generated.beans.Vehicle[] getAutoProfiles2()
  {
   Object __res = autoProfiles2_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Vehicle[])__res;
  }


  public void setAutoProfiles2(org.openl.generated.beans.Vehicle[] __var)
  {
   autoProfiles2_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField catPolicyScoring_Field;

  public org.openl.rules.table.properties.TableProperties getCatPolicyScoring()
  {
   Object __res = catPolicyScoring_Field.get(__instance, __env.get());
   return (org.openl.rules.table.properties.TableProperties)__res;
  }


  public void setCatPolicyScoring(org.openl.rules.table.properties.TableProperties __var)
  {
   catPolicyScoring_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField autoProfiles1_Field;

  public org.openl.generated.beans.Vehicle[] getAutoProfiles1()
  {
   Object __res = autoProfiles1_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Vehicle[])__res;
  }


  public void setAutoProfiles1(org.openl.generated.beans.Vehicle[] __var)
  {
   autoProfiles1_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField autoProfiles3_Field;

  public org.openl.generated.beans.Vehicle[] getAutoProfiles3()
  {
   Object __res = autoProfiles3_Field.get(__instance, __env.get());
   return (org.openl.generated.beans.Vehicle[])__res;
  }


  public void setAutoProfiles3(org.openl.generated.beans.Vehicle[] __var)
  {
   autoProfiles3_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField eligibility_type_Field;

  public java.lang.String[] getEligibility_type()
  {
   Object __res = eligibility_type_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setEligibility_type(java.lang.String[] __var)
  {
   eligibility_type_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField coverage_Field;

  public java.lang.String[] getCoverage()
  {
   Object __res = coverage_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setCoverage(java.lang.String[] __var)
  {
   coverage_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driverAgeTypeTest_Field;

  public org.openl.types.impl.DynamicObject[] getDriverAgeTypeTest()
  {
   Object __res = driverAgeTypeTest_Field.get(__instance, __env.get());
   return (org.openl.types.impl.DynamicObject[])__res;
  }


  public void setDriverAgeTypeTest(org.openl.types.impl.DynamicObject[] __var)
  {
   driverAgeTypeTest_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField driver_type_Field;

  public java.lang.String[] getDriver_type()
  {
   Object __res = driver_type_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setDriver_type(java.lang.String[] __var)
  {
   driver_type_Field.set(__instance, __var, __env.get());
  }



  static org.openl.types.IOpenField clientTier_Field;

  public java.lang.String[] getClientTier()
  {
   Object __res = clientTier_Field.get(__instance, __env.get());
   return (java.lang.String[])__res;
  }


  public void setClientTier(java.lang.String[] __var)
  {
   clientTier_Field.set(__instance, __var, __env.get());
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



  static org.openl.types.IOpenMethod vehicleTheftRating_Method;
  public java.lang.String vehicleTheftRating(org.openl.generated.beans.Vehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleTheftRating_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculateVehiclesPremium_Method;
  public org.openl.meta.DoubleValue calculateVehiclesPremium(java.lang.Object vehicles)  {
    Object[] __params = new Object[1];
    __params[0] = vehicles;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculateVehiclesPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleDiscount_Method;
  public org.openl.meta.DoubleValue vehicleDiscount(org.openl.generated.beans.Vehicle vehicle, java.lang.String vehicleTheftRating)  {
    Object[] __params = new Object[2];
    __params[0] = vehicle;
    __params[1] = vehicleTheftRating;
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleDiscount_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod clientDiscount_Method;
  public org.openl.meta.DoubleValue clientDiscount(org.openl.generated.beans.Policy policy)  {
    Object[] __params = new Object[1];
    __params[0] = policy;
    try
    {
    Object __myInstance = __instance;
    Object __res = clientDiscount_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculateDriversScore_Method;
  public org.openl.meta.DoubleValue calculateDriversScore(java.lang.Object drivers)  {
    Object[] __params = new Object[1];
    __params[0] = drivers;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculateDriversScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod processDrivers_Method;
  public org.openl.rules.calc.SpreadsheetResult[] processDrivers(org.openl.generated.beans.Driver[] drivers)  {
    Object[] __params = new Object[1];
    __params[0] = drivers;
    try
    {
    Object __myInstance = __instance;
    Object __res = processDrivers_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.SpreadsheetResult[])__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod processVehicle_Method;
  public org.openl.rules.calc.SpreadsheetResult processVehicle(org.openl.generated.beans.Vehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = processVehicle_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.SpreadsheetResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverRiskScore_Method;
  public org.openl.meta.DoubleValue driverRiskScore(java.lang.String driverRisk)  {
    Object[] __params = new Object[1];
    __params[0] = driverRisk;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRiskScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculateDriversPremium_Method;
  public org.openl.meta.DoubleValue calculateDriversPremium(java.lang.Object drivers)  {
    Object[] __params = new Object[1];
    __params[0] = drivers;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculateDriversPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod processPolicy_Method;
  public org.openl.rules.calc.SpreadsheetResult processPolicy(org.openl.generated.beans.Policy policy)  {
    Object[] __params = new Object[1];
    __params[0] = policy;
    try
    {
    Object __myInstance = __instance;
    Object __res = processPolicy_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.SpreadsheetResult)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleInjuryRating_Method;
  public java.lang.String vehicleInjuryRating(org.openl.generated.beans.Vehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleInjuryRating_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod processVehicles_Method;
  public org.openl.rules.calc.SpreadsheetResult[] processVehicles(org.openl.generated.beans.Vehicle[] vehicles)  {
    Object[] __params = new Object[1];
    __params[0] = vehicles;
    try
    {
    Object __myInstance = __instance;
    Object __res = processVehicles_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.SpreadsheetResult[])__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod theftRatingSurcharge_Method;
  public org.openl.meta.DoubleValue theftRatingSurcharge(java.lang.String theftRating)  {
    Object[] __params = new Object[1];
    __params[0] = theftRating;
    try
    {
    Object __myInstance = __instance;
    Object __res = theftRatingSurcharge_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverAgeTypeTestTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults driverAgeTypeTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = driverAgeTypeTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverEligibilityTestTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults driverEligibilityTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = driverEligibilityTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleEligibilityScore_Method;
  public org.openl.meta.DoubleValue vehicleEligibilityScore(java.lang.String vehicleEligibility)  {
    Object[] __params = new Object[1];
    __params[0] = vehicleEligibility;
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleEligibilityScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod policyEligibility_Method;
  public java.lang.String policyEligibility(org.openl.generated.beans.Policy policy, int score)  {
    Object[] __params = new Object[2];
    __params[0] = policy;
    __params[1] = new Integer(score);
    try
    {
    Object __myInstance = __instance;
    Object __res = policyEligibility_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleEligibility_Method;
  public java.lang.String vehicleEligibility(java.lang.String vehicleTheftRating, java.lang.String vehicleInjuryRating)  {
    Object[] __params = new Object[2];
    __params[0] = vehicleTheftRating;
    __params[1] = vehicleInjuryRating;
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleEligibility_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverRisk_Method;
  public java.lang.String driverRisk(org.openl.generated.beans.Driver driver)  {
    Object[] __params = new Object[1];
    __params[0] = driver;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRisk_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod currentYear_Method;
  public int currentYear()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = currentYear_Method.invoke(__myInstance, __params, __env.get());
   return ((Integer)__res).intValue();  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverAccidentPremium_Method;
  public org.openl.meta.DoubleValue driverAccidentPremium(org.openl.generated.beans.Driver driver, java.lang.String driverRisk)  {
    Object[] __params = new Object[2];
    __params[0] = driver;
    __params[1] = driverRisk;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverAccidentPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleInjuryRatingTestTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults vehicleInjuryRatingTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleInjuryRatingTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod basePrice_Method;
  public org.openl.meta.DoubleValue basePrice(org.openl.generated.beans.Vehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = basePrice_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverPremium_Method;
  public org.openl.meta.DoubleValue driverPremium(org.openl.generated.beans.Driver driver, java.lang.String driverAgeType)  {
    Object[] __params = new Object[2];
    __params[0] = driver;
    __params[1] = driverAgeType;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod injuryRatingSurcharge_Method;
  public org.openl.meta.DoubleValue injuryRatingSurcharge(java.lang.String injuryRating)  {
    Object[] __params = new Object[1];
    __params[0] = injuryRating;
    try
    {
    Object __myInstance = __instance;
    Object __res = injuryRatingSurcharge_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod vehicleTheftRatingTestTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults vehicleTheftRatingTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = vehicleTheftRatingTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod ageSurcharge_Method;
  public org.openl.meta.DoubleValue ageSurcharge(int vehicleAge)  {
    Object[] __params = new Object[1];
    __params[0] = new Integer(vehicleAge);
    try
    {
    Object __myInstance = __instance;
    Object __res = ageSurcharge_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverEligibility_Method;
  public java.lang.String driverEligibility(org.openl.generated.beans.Driver driver, java.lang.String ageType)  {
    Object[] __params = new Object[2];
    __params[0] = driver;
    __params[1] = ageType;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverEligibility_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod calculateVehiclesScore_Method;
  public org.openl.meta.DoubleValue calculateVehiclesScore(java.lang.Object vehicles)  {
    Object[] __params = new Object[1];
    __params[0] = vehicles;
    try
    {
    Object __myInstance = __instance;
    Object __res = calculateVehiclesScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverAgeType_Method;
  public java.lang.String driverAgeType(org.openl.generated.beans.Driver driver)  {
    Object[] __params = new Object[1];
    __params[0] = driver;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverAgeType_Method.invoke(__myInstance, __params, __env.get());
   return (java.lang.String)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverRiskPremium_Method;
  public org.openl.meta.DoubleValue driverRiskPremium(java.lang.String driverRisk)  {
    Object[] __params = new Object[1];
    __params[0] = driverRisk;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRiskPremium_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod clientTierScore_Method;
  public org.openl.meta.DoubleValue clientTierScore(org.openl.generated.beans.Policy policy)  {
    Object[] __params = new Object[1];
    __params[0] = policy;
    try
    {
    Object __myInstance = __instance;
    Object __res = clientTierScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverTypeScore_Method;
  public org.openl.meta.DoubleValue driverTypeScore(java.lang.String driverAgeType, java.lang.String driverEligibility)  {
    Object[] __params = new Object[2];
    __params[0] = driverAgeType;
    __params[1] = driverEligibility;
    try
    {
    Object __myInstance = __instance;
    Object __res = driverTypeScore_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod coverageSurcharge_Method;
  public org.openl.meta.DoubleValue coverageSurcharge(org.openl.generated.beans.Vehicle vehicle)  {
    Object[] __params = new Object[1];
    __params[0] = vehicle;
    try
    {
    Object __myInstance = __instance;
    Object __res = coverageSurcharge_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.meta.DoubleValue)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod driverRiskTestTestAll_Method;
  public org.openl.rules.testmethod.TestUnitsResults driverRiskTestTestAll()  {
    Object[] __params = new Object[0];
    try
    {
    Object __myInstance = __instance;
    Object __res = driverRiskTestTestAll_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.testmethod.TestUnitsResults)__res;  }
  catch(Throwable t)
  {
    Log.error("Java Wrapper execution error:", t);
    throw RuntimeExceptionWrapper.wrap(t);
  }

  }


  static org.openl.types.IOpenMethod processDriver_Method;
  public org.openl.rules.calc.SpreadsheetResult processDriver(org.openl.generated.beans.Driver driver)  {
    Object[] __params = new Object[1];
    __params[0] = driver;
    try
    {
    Object __myInstance = __instance;
    Object __res = processDriver_Method.invoke(__myInstance, __params, __env.get());
   return (org.openl.rules.calc.SpreadsheetResult)__res;  }
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
    IOpenSourceCodeModule source = OpenClassJavaWrapper.getSourceCodeModule(__src, ucxt);
    if (source != null) {
         source.setParams(__externalParams);
    }
    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , source, __executionMode, __dependencyManager);
    __compiledClass = wrapper.getCompiledClass();
    __class = wrapper.getOpenClassWithErrors();
   // __env.set(wrapper.getEnv());

    driverRisk_Field = __class.getField("driverRisk");
    driverEligibilityTest_Field = __class.getField("driverEligibilityTest");
    maritalStatus_Field = __class.getField("maritalStatus");
    theft_rating_Field = __class.getField("theft_rating");
    catVehiclePremium_Field = __class.getField("catVehiclePremium");
    car_type_Field = __class.getField("car_type");
    vehicleInjuryRatingTest_Field = __class.getField("vehicleInjuryRatingTest");
    policyProfile2_Field = __class.getField("policyProfile2");
    policyProfile1_Field = __class.getField("policyProfile1");
    airbag_type_Field = __class.getField("airbag_type");
    driverRiskTest_Field = __class.getField("driverRiskTest");
    policyProfile4_Field = __class.getField("policyProfile4");
    injury_rating_Field = __class.getField("injury_rating");
    policyProfile3_Field = __class.getField("policyProfile3");
    gender_Field = __class.getField("gender");
    driverProfiles1_Field = __class.getField("driverProfiles1");
    testVehicles1_Field = __class.getField("testVehicles1");
    testDrivers1_Field = __class.getField("testDrivers1");
    driverProfiles2_Field = __class.getField("driverProfiles2");
    moduleProp_Field = __class.getField("moduleProp");
    driverProfiles3_Field = __class.getField("driverProfiles3");
    testPolicy2_Field = __class.getField("testPolicy2");
    vehicleTheftRatingTest_Field = __class.getField("vehicleTheftRatingTest");
    testPolicy1_Field = __class.getField("testPolicy1");
    autoProfiles2_Field = __class.getField("autoProfiles2");
    catPolicyScoring_Field = __class.getField("catPolicyScoring");
    autoProfiles1_Field = __class.getField("autoProfiles1");
    autoProfiles3_Field = __class.getField("autoProfiles3");
    eligibility_type_Field = __class.getField("eligibility_type");
    coverage_Field = __class.getField("coverage");
    driverAgeTypeTest_Field = __class.getField("driverAgeTypeTest");
    driver_type_Field = __class.getField("driver_type");
    clientTier_Field = __class.getField("clientTier");
    this_Field = __class.getField("this");
    vehicleTheftRating_Method = __class.getMatchingMethod("vehicleTheftRating", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class)});
    calculateVehiclesPremium_Method = __class.getMatchingMethod("calculateVehiclesPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.Object.class)});
    vehicleDiscount_Method = __class.getMatchingMethod("vehicleDiscount", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    clientDiscount_Method = __class.getMatchingMethod("clientDiscount", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Policy.class)});
    calculateDriversScore_Method = __class.getMatchingMethod("calculateDriversScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.Object.class)});
    processDrivers_Method = __class.getMatchingMethod("processDrivers", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver[].class)});
    processVehicle_Method = __class.getMatchingMethod("processVehicle", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class)});
    driverRiskScore_Method = __class.getMatchingMethod("driverRiskScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    calculateDriversPremium_Method = __class.getMatchingMethod("calculateDriversPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.Object.class)});
    processPolicy_Method = __class.getMatchingMethod("processPolicy", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Policy.class)});
    vehicleInjuryRating_Method = __class.getMatchingMethod("vehicleInjuryRating", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class)});
    processVehicles_Method = __class.getMatchingMethod("processVehicles", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle[].class)});
    theftRatingSurcharge_Method = __class.getMatchingMethod("theftRatingSurcharge", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    driverAgeTypeTestTestAll_Method = __class.getMatchingMethod("driverAgeTypeTestTestAll", new IOpenClass[] {
});
    driverEligibilityTestTestAll_Method = __class.getMatchingMethod("driverEligibilityTestTestAll", new IOpenClass[] {
});
    vehicleEligibilityScore_Method = __class.getMatchingMethod("vehicleEligibilityScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    policyEligibility_Method = __class.getMatchingMethod("policyEligibility", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Policy.class),
      OpenClassHelper.getOpenClass(__class, int.class)});
    vehicleEligibility_Method = __class.getMatchingMethod("vehicleEligibility", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    driverRisk_Method = __class.getMatchingMethod("driverRisk", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class)});
    currentYear_Method = __class.getMatchingMethod("currentYear", new IOpenClass[] {
});
    driverAccidentPremium_Method = __class.getMatchingMethod("driverAccidentPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    vehicleInjuryRatingTestTestAll_Method = __class.getMatchingMethod("vehicleInjuryRatingTestTestAll", new IOpenClass[] {
});
    basePrice_Method = __class.getMatchingMethod("basePrice", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class)});
    driverPremium_Method = __class.getMatchingMethod("driverPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    injuryRatingSurcharge_Method = __class.getMatchingMethod("injuryRatingSurcharge", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    vehicleTheftRatingTestTestAll_Method = __class.getMatchingMethod("vehicleTheftRatingTestTestAll", new IOpenClass[] {
});
    ageSurcharge_Method = __class.getMatchingMethod("ageSurcharge", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, int.class)});
    driverEligibility_Method = __class.getMatchingMethod("driverEligibility", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    calculateVehiclesScore_Method = __class.getMatchingMethod("calculateVehiclesScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.Object.class)});
    driverAgeType_Method = __class.getMatchingMethod("driverAgeType", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class)});
    driverRiskPremium_Method = __class.getMatchingMethod("driverRiskPremium", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    clientTierScore_Method = __class.getMatchingMethod("clientTierScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Policy.class)});
    driverTypeScore_Method = __class.getMatchingMethod("driverTypeScore", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, java.lang.String.class),
      OpenClassHelper.getOpenClass(__class, java.lang.String.class)});
    coverageSurcharge_Method = __class.getMatchingMethod("coverageSurcharge", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Vehicle.class)});
    driverRiskTestTestAll_Method = __class.getMatchingMethod("driverRiskTestTestAll", new IOpenClass[] {
});
    processDriver_Method = __class.getMatchingMethod("processDriver", new IOpenClass[] {
      OpenClassHelper.getOpenClass(__class, org.openl.generated.beans.Driver.class)});

    __initialized=true;
  }
}