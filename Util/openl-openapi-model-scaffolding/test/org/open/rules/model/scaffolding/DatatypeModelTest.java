package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;

public class DatatypeModelTest {

    public static final String DRIVER = "Driver";
    public static final String HUMAN = "Human";

    @Test
    public void testSimpleCreationOfDataTypeModel() {
        DatatypeModel driver = new DatatypeModel(DRIVER);
        driver.setParent(HUMAN);
        assertEquals(DRIVER, driver.getName());
        assertEquals(HUMAN, driver.getParent());
        assertEquals(driver, driver);
        assertEquals(driver.hashCode(), driver.hashCode());
        assertNotEquals(driver, null);

        DatatypeModel truckDriver = new DatatypeModel(DRIVER);
        truckDriver.setParent(HUMAN);
        assertEquals(DRIVER, truckDriver.getName());
        assertEquals(HUMAN, truckDriver.getParent());
        assertEquals(driver, truckDriver);
        assertEquals(driver.hashCode(), truckDriver.hashCode());

        DatatypeModel human = new DatatypeModel(HUMAN);
        assertNotEquals(driver, human);
        assertNotEquals(driver.hashCode(), human.hashCode());

        DatatypeModel goalkeeper = new DatatypeModel("Goalkeeper");
        goalkeeper.setParent(HUMAN);
        assertNotEquals(driver, goalkeeper);
        assertNotEquals(driver.hashCode(), goalkeeper.hashCode());

        DatatypeModel defender = new DatatypeModel("lb");
        defender.setName("defender");
        assertEquals(defender.getName(), "defender");
    }

    @Test
    public void testDataTypeModelWithOneField() {
        DatatypeModel dm = new DatatypeModel(DRIVER);
        DatatypeModel oneMoreDm = new DatatypeModel(DRIVER);
        assertEquals(dm, oneMoreDm);
        assertEquals(dm.hashCode(), oneMoreDm.hashCode());

        FieldModel height = new FieldModel("height", "String");

        dm.setFields(Collections.singletonList(height));
        oneMoreDm.setFields(Collections.singletonList(height));
        assertEquals(dm, oneMoreDm);
        assertEquals(dm.hashCode(), oneMoreDm.hashCode());
    }

    @Test
    public void testDataTypeModelWithManySameFields() {
        DatatypeModel driver = new DatatypeModel(DRIVER);
        DatatypeModel oneMoreDriver = new DatatypeModel(DRIVER);
        assertEquals(driver, oneMoreDriver);
        assertEquals(driver.hashCode(), oneMoreDriver.hashCode());

        FieldModel height = new FieldModel("height", "String");
        FieldModel weight = new FieldModel("weight", "Double");
        FieldModel size = new FieldModel("size", "Long");

        List<FieldModel> fields = Arrays.asList(height, weight, size);
        List<FieldModel> oneMoreFields = Arrays.asList(height, weight, size);

        driver.setFields(fields);
        oneMoreDriver.setFields(oneMoreFields);
        assertEquals(3, driver.getFields().size());
        assertEquals(3, oneMoreDriver.getFields().size());
        assertEquals(driver, oneMoreDriver);
        assertEquals(driver.hashCode(), oneMoreDriver.hashCode());
    }

    @Test
    public void testDataTypeModelWithDifferentFields() {
        DatatypeModel driver = new DatatypeModel(DRIVER);
        DatatypeModel oneMoreDriver = new DatatypeModel(DRIVER);

        FieldModel speed = new FieldModel("speed", "Integer");
        FieldModel carColor = new FieldModel("carColor", "String");
        FieldModel licenseNumber = new FieldModel("licenseNumber", "String");
        FieldModel passportId = new FieldModel("passportID", "UUID");

        driver.setFields(Arrays.asList(speed, carColor, licenseNumber, passportId));
        oneMoreDriver.setFields(Arrays.asList(speed, licenseNumber));
        assertEquals(4, driver.getFields().size());
        assertEquals(2, oneMoreDriver.getFields().size());
        assertNotEquals(driver, oneMoreDriver);
        assertNotEquals(driver.hashCode(), oneMoreDriver.hashCode());
    }

}
