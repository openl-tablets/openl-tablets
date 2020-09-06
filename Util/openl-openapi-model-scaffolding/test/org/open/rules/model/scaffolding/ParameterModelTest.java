package org.open.rules.model.scaffolding;

import org.junit.Test;
import org.openl.rules.model.scaffolding.ParameterModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ParameterModelTest {

    @Test
    public void testParameterModelCreation() {
        ParameterModel nameParam = new ParameterModel("String", "name");
        ParameterModel oneMoreNameParam = new ParameterModel("String", "name");
        ParameterModel surnameParam = new ParameterModel("String", "surname");
        ParameterModel integerParam = new ParameterModel();

        assertEquals(nameParam, nameParam);
        assertEquals(nameParam, oneMoreNameParam);
        assertEquals(nameParam.hashCode(), oneMoreNameParam.hashCode());

        assertNotEquals(nameParam, null);
        assertNotEquals(nameParam, surnameParam);
        assertNotEquals(nameParam.hashCode(), surnameParam.hashCode());

        integerParam.setName("name");
        integerParam.setType("Integer");
        assertEquals("name", integerParam.getName());
        assertEquals("Integer", integerParam.getType());
        assertNotEquals(nameParam, integerParam);
        assertNotEquals(nameParam.hashCode(), integerParam.hashCode());

        assertEquals(nameParam.getName(), "name");
        assertEquals(nameParam.getType(), "String");
    }
}
