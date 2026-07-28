package org.open.rules.model.scaffolding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;

class DatatypeModelTest {

    private static final String DRIVER = "Driver";
    private static final String HUMAN = "Human";

    @Test
    void testSimpleCreationOfDataTypeModel() {
        var driver = new DatatypeModel(DRIVER);
        driver.setParent(HUMAN);
        assertEquals(DRIVER, driver.getName());
        assertEquals(HUMAN, driver.getParent());
        assertEquals(driver, driver);
        assertEquals(driver.hashCode(), driver.hashCode());
        assertNotEquals(driver, null);

        var truckDriver = new DatatypeModel(DRIVER);
        truckDriver.setParent(HUMAN);
        assertEquals(DRIVER, truckDriver.getName());
        assertEquals(HUMAN, truckDriver.getParent());
        assertEquals(driver, truckDriver);
        assertEquals(driver.hashCode(), truckDriver.hashCode());

        var human = new DatatypeModel(HUMAN);
        assertNotEquals(driver, human);
        assertNotEquals(driver.hashCode(), human.hashCode());

        var goalkeeper = new DatatypeModel("Goalkeeper");
        goalkeeper.setParent(HUMAN);
        assertNotEquals(driver, goalkeeper);
        assertNotEquals(driver.hashCode(), goalkeeper.hashCode());

        var defender = new DatatypeModel("lb");
        defender.setName("defender");
        assertEquals(defender.getName(), "defender");
    }

    @Test
    void testDataTypeModelWithOneField() {
        var dm = new DatatypeModel(DRIVER);
        var oneMoreDm = new DatatypeModel(DRIVER);
        assertEquals(dm, oneMoreDm);
        assertEquals(dm.hashCode(), oneMoreDm.hashCode());

        var height = new FieldModel("height", "String");

        dm.setFields(List.of(height));
        oneMoreDm.setFields(List.of(height));
        assertEquals(dm, oneMoreDm);
        assertEquals(dm.hashCode(), oneMoreDm.hashCode());
    }

    @Test
    void testDataTypeModelWithManySameFields() {
        var driver = new DatatypeModel(DRIVER);
        var oneMoreDriver = new DatatypeModel(DRIVER);
        assertEquals(driver, oneMoreDriver);
        assertEquals(driver.hashCode(), oneMoreDriver.hashCode());

        var height = new FieldModel("height", "String");
        var weight = new FieldModel("weight", "Double");
        var size = new FieldModel("size", "Long");

        var fields = Arrays.asList(height, weight, size);
        var oneMoreFields = Arrays.asList(height, weight, size);

        driver.setFields(fields);
        oneMoreDriver.setFields(oneMoreFields);
        assertEquals(3, driver.getFields().size());
        assertEquals(3, oneMoreDriver.getFields().size());
        assertEquals(driver, oneMoreDriver);
        assertEquals(driver.hashCode(), oneMoreDriver.hashCode());
    }

    @Test
    void testDataTypeModelWithDifferentFields() {
        var driver = new DatatypeModel(DRIVER);
        var oneMoreDriver = new DatatypeModel(DRIVER);

        var speed = new FieldModel("speed", "Integer");
        var carColor = new FieldModel("carColor", "String");
        var licenseNumber = new FieldModel("licenseNumber", "String");
        var passportId = new FieldModel("passportID", "UUID");

        driver.setFields(Arrays.asList(speed, carColor, licenseNumber, passportId));
        oneMoreDriver.setFields(Arrays.asList(speed, licenseNumber));
        assertEquals(4, driver.getFields().size());
        assertEquals(2, oneMoreDriver.getFields().size());
        assertNotEquals(driver, oneMoreDriver);
        assertNotEquals(driver.hashCode(), oneMoreDriver.hashCode());
    }

}
