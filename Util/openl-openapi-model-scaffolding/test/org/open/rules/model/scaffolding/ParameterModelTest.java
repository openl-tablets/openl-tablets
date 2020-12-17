package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.rules.model.scaffolding.TypeInfo;

public class ParameterModelTest {

    @Test
    public void testParameterModelCreation() {
        ParameterModel nameParam = new ParameterModel(new TypeInfo(String.class.getName(), "String"), "name");
        ParameterModel oneMoreNameParam = new ParameterModel(new TypeInfo(String.class.getName(), "String"), "name");
        ParameterModel surnameParam = new ParameterModel(new TypeInfo(String.class.getName(), "String"), "surname");
        ParameterModel integerParam = new ParameterModel();

        assertEquals(nameParam, nameParam);
        assertEquals(nameParam, oneMoreNameParam);
        assertEquals(nameParam.hashCode(), oneMoreNameParam.hashCode());

        assertNotEquals(nameParam, null);
        assertNotEquals(nameParam, surnameParam);
        assertNotEquals(nameParam.hashCode(), surnameParam.hashCode());

        integerParam.setName("name");
        integerParam.setType(new TypeInfo(Integer.class.getName(), "Integer", false));
        assertEquals("name", integerParam.getName());
        assertEquals("Integer", integerParam.getType().getSimpleName());
        assertNotEquals(nameParam, integerParam);
        assertNotEquals(nameParam.hashCode(), integerParam.hashCode());

        assertEquals("name", nameParam.getName());
        assertEquals("String", nameParam.getType().getSimpleName());
    }
}
