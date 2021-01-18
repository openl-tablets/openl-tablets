package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.TypeInfo;

public class ParameterModelTest {

    @Test
    public void testParameterModelCreation() {
        ParameterModel nameParam = new ParameterModel(new TypeInfo(String.class), "name");
        ParameterModel oneMoreNameParam = new ParameterModel(new TypeInfo(String.class), "name");
        ParameterModel surnameParam = new ParameterModel(new TypeInfo(String.class), "surname");


        assertEquals(nameParam, nameParam);
        assertEquals(nameParam, oneMoreNameParam);
        assertEquals(nameParam.hashCode(), oneMoreNameParam.hashCode());

        assertNotEquals(nameParam, null);
        assertNotEquals(nameParam, surnameParam);
        assertNotEquals(nameParam.hashCode(), surnameParam.hashCode());

        ParameterModel integerParam = new ParameterModel(new TypeInfo(Integer.class), "name");
        assertEquals("name", integerParam.getFormattedName());
        assertEquals("Integer", integerParam.getType().getSimpleName());
        assertNotEquals(nameParam, integerParam);
        assertNotEquals(nameParam.hashCode(), integerParam.hashCode());

        assertEquals("name", nameParam.getFormattedName());
        assertEquals("String", nameParam.getType().getSimpleName());
    }
}
