package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.TestUtils;

class SimpleDTCompoundResultTest {
    private static Object instance;

    @BeforeAll
    static void init() {
        instance = TestUtils.create("test/rules/dt/SimpleDTCompoundResultTest.xls");
    }

    @Test
    void test1() {
        Object result = TestUtils.invoke(instance, "test1", 1);
        String policyNumber = TestUtils.invoke(result, "getPolicyNumber");
        assertEquals("Policy Number 2", policyNumber);

        Object vehicle = TestUtils.invoke(result, "getVehicle");
        assertNotNull(vehicle);
        String vehicleName = TestUtils.invoke(vehicle, "getVehicleName");
        assertEquals("Vehicle Name 2", vehicleName);

        Object oldDriver = TestUtils.invoke(vehicle, "getOldDriver");
        assertNotNull(oldDriver);

        String oldDriverName = TestUtils.invoke(oldDriver, "getDriverName");
        assertEquals("Driver Name 2", oldDriverName);
    }

    @Test
    void test2() {
        Object result = TestUtils.invoke(instance, "test2", 1);
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

        var id = TestUtils.invoke(oldDriver, "getDriverID");
        assertEquals(1, id);

    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"test3", "test4", "test5"})
    void testNewAndOldDriver(String rule) {
        Object result = TestUtils.invoke(instance, rule, 1);
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
