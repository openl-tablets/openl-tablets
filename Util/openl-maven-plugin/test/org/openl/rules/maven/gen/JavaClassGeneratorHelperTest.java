package org.openl.rules.maven.gen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.util.StringUtils;

public class JavaClassGeneratorHelperTest {

    @Test
    public void testFilterTypeName() {
        assertEquals("java.lang.String", JavaClassGeneratorHelper.filterTypeName(String.class));
        assertEquals("java.lang.String[]", JavaClassGeneratorHelper.filterTypeName(String[].class));
        assertEquals("java.lang.String[][]", JavaClassGeneratorHelper.filterTypeName(String[][].class));
        assertEquals("int", JavaClassGeneratorHelper.filterTypeName(int.class));
        assertEquals("int[][]", JavaClassGeneratorHelper.filterTypeName(int[][].class));

        assertEquals(StringUtils.EMPTY, JavaClassGeneratorHelper.filterTypeName(null));
    }

    @Test
    public void testGetPackageText() {
        assertEquals("package org.openl.commons;\n\n", JavaClassGeneratorHelper.getPackageText("org.openl.commons"));
        assertEquals(StringUtils.EMPTY, JavaClassGeneratorHelper.getPackageText(null));
    }

    @Test
    public void testGetTypeNameForCast() {
        assertEquals("java.lang.String", JavaClassGeneratorHelper.getTypeNameForCastFromObject(String.class));

        assertEquals("java.lang.String[]", JavaClassGeneratorHelper.getTypeNameForCastFromObject(String[].class));

        assertEquals("For primitives wrapper type should be used", "java.lang.Integer",
                JavaClassGeneratorHelper.getTypeNameForCastFromObject(int.class));

        assertEquals("int[]", JavaClassGeneratorHelper.getTypeNameForCastFromObject(int[].class));

        assertEquals(StringUtils.EMPTY, JavaClassGeneratorHelper.getTypeNameForCastFromObject(null));
    }

    @Test
    public void testGetterWithCastMethod() {
        assertEquals(
                "  public java.lang.String getMyField() {\n   return (java.lang.String)getFieldValue(\"myField\");\n}\n",
                JavaClassGeneratorHelper.getGetterWithCastMethod(String.class, "getFieldValue", "myField"));

        assertEquals(
                "  public int getMyField() {\n   return ((java.lang.Integer)getFieldValue(\"myField\")).intValue();\n}\n",
                JavaClassGeneratorHelper.getGetterWithCastMethod(int.class, "getFieldValue", "myField"));

        assertEquals(
                "  public double getMyField() {\n   return ((java.lang.Double)getFieldValue(\"myField\")).doubleValue();\n}\n",
                JavaClassGeneratorHelper.getGetterWithCastMethod(double.class, "getFieldValue", "myField"));

    }
}
