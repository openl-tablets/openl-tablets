package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class SmartDTCompoundResultTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/SmartDTCompoundResultTest.xls";

    public SmartDTCompoundResultTest() {
        super(SRC);
    }
    
    @Test
    public void test1() throws ClassNotFoundException,
                                        NoSuchMethodException,
                                        InvocationTargetException,
                                        IllegalAccessException {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("test1",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(1) , "XXX"}, env);
        Class<?> policyClass = getClass("org.openl.generated.beans.Policy");
        Method getPolicyNumberMethod = policyClass.getMethod("getPolicyNumber");
        String policyNumber = (String) getPolicyNumberMethod.invoke(result);
        assertEquals("Policy Number 2", policyNumber);
        
        Method getVehicleMethod = policyClass.getMethod("getVehicle");
        Object vehicle = getVehicleMethod.invoke(result);
        assertNotNull(vehicle);
        Method getVehicleNameMethod = vehicle.getClass().getMethod("getVehicleName");
        String vehicleName = (String) getVehicleNameMethod.invoke(vehicle);
        assertEquals("Vehicle Name 2", vehicleName);
        
        Method getOldDriverMethod = vehicle.getClass().getMethod("getOldDriver");
        Object oldDriver = getOldDriverMethod.invoke(vehicle);
        assertNotNull(oldDriver);
        
        Method getDriverNameMethod = oldDriver.getClass().getMethod("getDriverName");
        String oldDriverName = (String) getDriverNameMethod.invoke(oldDriver);
        assertEquals("XXX", oldDriverName);
    }
    
    @Test
    public void test2() throws ClassNotFoundException,
                                        NoSuchMethodException,
                                        InvocationTargetException,
                                        IllegalAccessException {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("test2",
            new IOpenClass[] { JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(String.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { "XXX", new Integer(1), null }, env);
        Class<?> policyClass = getClass("org.openl.generated.beans.Policy");
        Method getPolicyNumberMethod = policyClass.getMethod("getPolicyNumber");
        String policyNumber = (String) getPolicyNumberMethod.invoke(result);
        assertEquals("Policy Number 2", policyNumber);
        
        Method getVehicleMethod = policyClass.getMethod("getVehicle");
        Object vehicle = getVehicleMethod.invoke(result);
        assertNotNull(vehicle);
        Method getVehicleNameMethod = vehicle.getClass().getMethod("getVehicleName");
        String vehicleName = (String) getVehicleNameMethod.invoke(vehicle);
        assertEquals("Vehicle Name 2", vehicleName);
        
        Method getNewDriverMethod = vehicle.getClass().getMethod("getNewDriver");
        Object newDriver = getNewDriverMethod.invoke(vehicle);
        assertNotNull(newDriver);
        
        Method getDriverNameMethod = newDriver.getClass().getMethod("getDriverName");
        String newDriverName = (String) getDriverNameMethod.invoke(newDriver);
        assertEquals("XXX", newDriverName);
        
        Method getOldDriverMethod = vehicle.getClass().getMethod("getOldDriver");
        Object oldDriver = getOldDriverMethod.invoke(vehicle);
        assertNotNull(oldDriver);

        Method getDriverIDMethod = oldDriver.getClass().getMethod("getDriverID");
        Integer id = (Integer) getDriverIDMethod.invoke(oldDriver);
        assertEquals(Integer.valueOf(1), id);

    }
    
    @Test
    public void test3() throws ClassNotFoundException,
                                        NoSuchMethodException,
                                        InvocationTargetException,
                                        IllegalAccessException {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("test3",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(1) }, env);
        Class<?> policyClass = getClass("org.openl.generated.beans.Policy");
        Method getPolicyNumberMethod = policyClass.getMethod("getPolicyNumber");
        String policyNumber = (String) getPolicyNumberMethod.invoke(result);
        assertEquals("Policy Number 2", policyNumber);
        
        Method getVehicleMethod = policyClass.getMethod("getVehicle");
        Object vehicle = getVehicleMethod.invoke(result);
        assertNotNull(vehicle);
        Method getVehicleNameMethod = vehicle.getClass().getMethod("getVehicleName");
        String vehicleName = (String) getVehicleNameMethod.invoke(vehicle);
        assertEquals("Vehicle Name 2", vehicleName);
        
        Method getNewDriverMethod = vehicle.getClass().getMethod("getNewDriver");
        Object newDriver = getNewDriverMethod.invoke(vehicle);
        assertNotNull(newDriver);
        
        Method getDriverNameMethod = newDriver.getClass().getMethod("getDriverName");
        String newDriverName = (String) getDriverNameMethod.invoke(newDriver);
        assertEquals("New Driver Name 2", newDriverName);
        
        Method getOldDriverMethod = vehicle.getClass().getMethod("getOldDriver");
        Object oldDriver = getOldDriverMethod.invoke(vehicle);
        assertNotNull(oldDriver);

        String oldDriverName = (String) getDriverNameMethod.invoke(oldDriver);
        assertEquals("Old Driver Name 2", oldDriverName);
    }
    
    @Test
    public void test4() throws ClassNotFoundException,
                                        NoSuchMethodException,
                                        InvocationTargetException,
                                        IllegalAccessException {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("test4",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(1) }, env);
        Class<?> policyClass = getClass("org.openl.generated.beans.Policy");
        Method getPolicyNumberMethod = policyClass.getMethod("getPolicyNumber");
        String policyNumber = (String) getPolicyNumberMethod.invoke(result);
        assertEquals("Policy Number 2", policyNumber);
        
        Method getVehicleMethod = policyClass.getMethod("getVehicle");
        Object vehicle = getVehicleMethod.invoke(result);
        assertNotNull(vehicle);
        Method getVehicleNameMethod = vehicle.getClass().getMethod("getVehicleName");
        String vehicleName = (String) getVehicleNameMethod.invoke(vehicle);
        assertEquals("Vehicle Name 2", vehicleName);
        
        Method getNewDriverMethod = vehicle.getClass().getMethod("getNewDriver");
        Object newDriver = getNewDriverMethod.invoke(vehicle);
        assertNotNull(newDriver);
        
        Method getDriverNameMethod = newDriver.getClass().getMethod("getDriverName");
        String newDriverName = (String) getDriverNameMethod.invoke(newDriver);
        assertEquals("New Driver Name 2", newDriverName);
        
        Method getOldDriverMethod = vehicle.getClass().getMethod("getOldDriver");
        Object oldDriver = getOldDriverMethod.invoke(vehicle);
        assertNotNull(oldDriver);

        String oldDriverName = (String) getDriverNameMethod.invoke(oldDriver);
        assertEquals("Old Driver Name 2", oldDriverName);
    }
    
    @Test
    public void test5() throws ClassNotFoundException,
                                        NoSuchMethodException,
                                        InvocationTargetException,
                                        IllegalAccessException {
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("test5",
            new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object result = method.invoke(instance, new Object[] { new Integer(1) }, env);
        Class<?> policyClass = getClass("org.openl.generated.beans.Policy");
        Method getPolicyNumberMethod = policyClass.getMethod("getPolicyNumber");
        String policyNumber = (String) getPolicyNumberMethod.invoke(result);
        assertEquals("Policy Number 2", policyNumber);
        
        Method getVehicleMethod = policyClass.getMethod("getVehicle");
        Object vehicle = getVehicleMethod.invoke(result);
        assertNotNull(vehicle);
        Method getVehicleNameMethod = vehicle.getClass().getMethod("getVehicleName");
        String vehicleName = (String) getVehicleNameMethod.invoke(vehicle);
        assertEquals("Vehicle Name 2", vehicleName);
        
        Method getNewDriverMethod = vehicle.getClass().getMethod("getNewDriver");
        Object newDriver = getNewDriverMethod.invoke(vehicle);
        assertNotNull(newDriver);
        
        Method getDriverNameMethod = newDriver.getClass().getMethod("getDriverName");
        String newDriverName = (String) getDriverNameMethod.invoke(newDriver);
        assertEquals("New Driver Name 2", newDriverName);
        
        Method getOldDriverMethod = vehicle.getClass().getMethod("getOldDriver");
        Object oldDriver = getOldDriverMethod.invoke(vehicle);
        assertNotNull(oldDriver);

        String oldDriverName = (String) getDriverNameMethod.invoke(oldDriver);
        assertEquals("Old Driver Name 2", oldDriverName);
    }
}
