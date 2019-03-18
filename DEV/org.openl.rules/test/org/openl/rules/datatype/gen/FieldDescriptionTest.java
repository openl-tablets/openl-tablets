package org.openl.rules.datatype.gen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.gen.FieldDescription;
import org.openl.gen.writers.DefaultValue;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;

/**
 * Created by dl on 6/18/14.
 */
public class FieldDescriptionTest {

    public static final String DEFAULT_STRING_VALUE = "Default value";
    public static final String DEFAULT_BOOLEAN_VALUE = "true";
    public static final String DEFAULT_INTEGER_VALUE = "25";

    @Test
    public void testDefaultValue_String() {
        FieldDescription field = FieldDescriptionBuilder.create(String.class.getName())
            .setDefaultValueAsString(DEFAULT_STRING_VALUE)
            .build();
        assertEquals(DEFAULT_STRING_VALUE, field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_Boolean() {
        FieldDescription field = FieldDescriptionBuilder.create(Boolean.class.getName())
            .setDefaultValueAsString(DEFAULT_BOOLEAN_VALUE)
            .build();
        assertEquals(Boolean.TRUE, field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_Integer() {
        FieldDescription field = FieldDescriptionBuilder.create(Integer.class.getName())
            .setDefaultValueAsString(DEFAULT_INTEGER_VALUE)
            .build();
        assertEquals(Integer.valueOf(25), field.getDefaultValue());
    }

    @Test
    public void testDefaultValue_DefaultBean() {
        FieldDescription field = FieldDescriptionBuilder.create(String.class.getName())
            .setDefaultValueAsString(DefaultValue.DEFAULT)
            .build();
        assertEquals("Return the default keyword itself", DefaultValue.DEFAULT, field.getDefaultValue());

        FieldDescription field1 = FieldDescriptionBuilder.create(Boolean.class.getName())
            .setDefaultValueAsString(DefaultValue.DEFAULT)
            .build();
        assertEquals("Return the default keyword itself", DefaultValue.DEFAULT, field1.getDefaultValue());

        FieldDescription field2 = FieldDescriptionBuilder.create(Integer.class.getName())
            .setDefaultValueAsString(DefaultValue.DEFAULT)
            .build();
        assertEquals("Return the default keyword itself", DefaultValue.DEFAULT, field2.getDefaultValue());
    }

    @Test
    public void testArrayOpenClass() {
        // Create the IOpenClass for the policy
        //
        DatatypeOpenClass policyClass = new DatatypeOpenClass(Policy.class.getSimpleName(),
            Policy.class.getPackage().getName());
        policyClass.setInstanceClass(Policy.class);

        // Create the IOpenClass for the Driver
        //
        DatatypeOpenClass driverClass = new DatatypeOpenClass(Driver.class.getSimpleName(),
            Driver.class.getPackage().getName());
        driverClass.setInstanceClass(Driver.class);

        // Create the IOpenClass for the drivers[]
        //
        IOpenClass driversClass = DynamicArrayAggregateInfo.aggregateInfo.getIndexedAggregateType(driverClass);

        // Create the field that belongs to the policy and contains drivers
        //
        DatatypeOpenField driversField = new DatatypeOpenField(policyClass, "drivers", driversClass);

        FieldDescription field = new FieldDescription(driversField.getType().getInstanceClass().getName());
        assertEquals(Driver[].class.getName(), field.getTypeName());
    }

    @Test
    public void testGetJavaType() {
        assertEquals("Ljava/lang/String;", new FieldDescription(String.class.getName()).getTypeDescriptor());
        assertEquals("[Ljava/lang/String;", new FieldDescription(String[].class.getName()).getTypeDescriptor());
        assertEquals("I", new FieldDescription(int.class.getName()).getTypeDescriptor());
        assertEquals("[[I", new FieldDescription(int[][].class.getName()).getTypeDescriptor());
        assertEquals("D", new FieldDescription(double.class.getName()).getTypeDescriptor());
        assertEquals("Ljava/lang/String;", new FieldDescription(String.class.getName()).getTypeDescriptor());
        assertEquals("I", new FieldDescription(int.class.getName()).getTypeDescriptor());
        assertEquals("B", new FieldDescription(byte.class.getName()).getTypeDescriptor());
        assertEquals("S", new FieldDescription(short.class.getName()).getTypeDescriptor());
        assertEquals("J", new FieldDescription(long.class.getName()).getTypeDescriptor());
        assertEquals("F", new FieldDescription(float.class.getName()).getTypeDescriptor());
        assertEquals("D", new FieldDescription(double.class.getName()).getTypeDescriptor());
        assertEquals("Z", new FieldDescription(boolean.class.getName()).getTypeDescriptor());
        assertEquals("C", new FieldDescription(char.class.getName()).getTypeDescriptor());
        assertEquals("[[Lorg/test/MyType;", new FieldDescription("[[Lorg.test.MyType;").getTypeDescriptor());
        assertEquals("Lorg/test/MyType;", new FieldDescription("org.test.MyType").getTypeDescriptor());
    }

    public static class Policy {

    }

    public static class Driver {

    }
}
