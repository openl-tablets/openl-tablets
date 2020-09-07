package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.FieldModel;

public class FieldModelTest {

    @Test
    public void testFieldModelCreation() {
        FieldModel fm = new FieldModel("type", "String");
        assertEquals("type", fm.getName());
        assertEquals("String", fm.getType());
        assertEquals(fm, fm);
        assertEquals(fm.hashCode(), fm.hashCode());
        assertNotEquals(fm, null);

        FieldModel fmd = new FieldModel("Sum", "Integer", 0);
        assertEquals("Sum", fmd.getName());
        assertEquals("Integer", fmd.getType());
        assertEquals(0, fmd.getDefaultValue());

        assertNotEquals(fmd, fm);
        assertNotEquals(fmd.hashCode(), fm.hashCode());

        FieldModel weightFieldString = new FieldModel("weight", "String", "10");
        FieldModel weightFieldInteger = new FieldModel("weight", "Integer", 0);

        assertNotEquals(weightFieldInteger, weightFieldString);
        assertNotEquals(weightFieldInteger.hashCode(), weightFieldString.hashCode());

        FieldModel heightModel = new FieldModel("height", "String", "193");
        FieldModel heightModelLow = new FieldModel("height", "String", "-134");

        assertNotEquals(heightModel, heightModelLow);
        assertNotEquals(heightModel.hashCode(), heightModelLow.hashCode());
    }
}
