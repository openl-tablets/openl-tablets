package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.TestUtils;

public class SmartDTCompoundResultTest {
    private static Object instance;

    @BeforeClass
    public static void init() {
        instance = TestUtils.create("test/rules/dt/SmartDTCompoundResultTest.xls");
    }

    @Test
    public void test1() {
        Object result = TestUtils.invoke(instance, "test1", 1, "XXX");
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        String oldDriverName = TestUtils.invoke(oldDriver, "getDriverName");
        assertEquals("XXX", oldDriverName);
    }

    @Test
    public void test2() {
        Object result = TestUtils.invoke(instance, "test2", "XXX", 1, "");
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object newDriver = TestUtils.invoke(vehicle, "getNewDriver");
        assertNotNull(newDriver);

        String newDriverName = TestUtils.invoke(newDriver, "getDriverName");
        assertEquals("XXX", newDriverName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        int id = TestUtils.invoke(oldDriver, "getDriverID");
        assertEquals(1, id);
    }

    @Test
    public void test3() {
        Object result = TestUtils.invoke(instance, "test3", 1);
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object newDriver = TestUtils.invoke(vehicle, "getNewDriver");
        assertNotNull(newDriver);

        String newDriverName = TestUtils.invoke(newDriver, "getDriverName");
        assertEquals("New Driver Name 2", newDriverName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        String oldDriverName = TestUtils.invoke(oldDriver, "getDriverName");
        assertEquals("Old Driver Name 2", oldDriverName);
    }

    @Test
    public void test4() {
        Object result = TestUtils.invoke(instance, "test4", 1);
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object newDriver = TestUtils.invoke(vehicle, "getNewDriver");
        assertNotNull(newDriver);

        String newDriverName = TestUtils.invoke(newDriver, "getDriverName");
        assertEquals("New Driver Name 2", newDriverName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        String oldDriverName = TestUtils.invoke(oldDriver, "getDriverName");
        assertEquals("Old Driver Name 2", oldDriverName);
    }

    @Test
    public void test5() {
        Object result = TestUtils.invoke(instance, "test5", 1);
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object newDriver = TestUtils.invoke(vehicle, "getNewDriver");
        assertNotNull(newDriver);

        String newDriverName = TestUtils.invoke(newDriver, "getDriverName");
        assertEquals("New Driver Name 2", newDriverName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        String oldDriverName = TestUtils.invoke(oldDriver, "getDriverName");
        assertEquals("Old Driver Name 2", oldDriverName);
    }
}
