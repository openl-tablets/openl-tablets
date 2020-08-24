package org.openl.codegen;

import org.junit.Assert;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.java.JavaOpenClass;

import junit.framework.TestCase;

public class JavaCodeGenTest extends TestCase {

    public void testGenClass() {
        // fail("Not yet implemented");
    }

    public void testGenField() {
        // fail("Not yet implemented");
    }

    public void testGenLiteralArray() {
        Assert.assertEquals("new Object[] {}", cg.genLiteralArray(new Object[] {}, ctr, sb()).toString());
        Assert.assertEquals("new Object[] {1, 'a'}", cg.genLiteralArray(new Object[] { 1, 'a' }, ctr, sb()).toString());
        Assert.assertEquals("new Object[][] {new Object[] {1, 'a'}, new Object[] {\"xxx\"}}",
            cg.genLiteralArray(new Object[][] { { 1, 'a' }, { "xxx" } }, ctr, sb()).toString());

        Assert.assertEquals("new Object[][] {new Object[] {1, 'a', 3.0, 255L}, new Object[] {\"xxx\"}}",
            cg.genLiteralArray(new Object[][] { { 1, 'a', 3.0, 255L }, { "xxx" } }, ctr, sb()).toString());
    }

    public void testGenLiteralChar() {
        Assert.assertEquals("'\\''", cg.genLiteralChar('\'', sb()).toString());
    }

    public void testGenLiteralDouble() {
        cg.setDoublePrecision(4);
        Assert.assertEquals("12.0", cg.genLiteralDouble(12.0, sb()).toString());
        Assert.assertEquals("11.99", cg.genLiteralDouble(11.99, sb()).toString());
        Assert.assertEquals("3.3333", cg.genLiteralDouble(10.0 / 3, sb()).toString());
        Assert.assertEquals("6.6667", cg.genLiteralDouble(20.0 / 3, sb()).toString());
        cg.setDoublePrecision(2);
        Assert.assertEquals("6.67", cg.genLiteralDouble(20.0 / 3, sb()).toString());
        cg.setDoublePrecision(5);
        Assert.assertEquals("6.66667", cg.genLiteralDouble(20.0 / 3, sb()).toString());
    }

    public void testGenLiteralInt() {
        Assert.assertEquals("554", cg.genLiteralInt(554, sb()).toString());

    }

    public void testGenLiteralString() {
        Assert.assertEquals("\"a\"", cg.genLiteralString("a", sb()).toString());
        Assert.assertEquals("\"a\\'\\\"\\n\"", cg.genLiteralString("a\'\"\n", sb()).toString());
        String baseLongStr = "0123456789";
        String longStr = makeLongStr(baseLongStr, 20);
        Assert.assertEquals(
            "\"" + makeLongStr(baseLongStr, 8) + "\"\r\n" + " + \"" + makeLongStr(baseLongStr,
                8) + "\"\r\n" + " + \"" + makeLongStr(baseLongStr, 4) + "\"",
            cg.genLiteralString(longStr, sb()).toString());
        Assert.assertEquals("null", cg.genLiteralString(null, sb()).toString());
    }

    String makeLongStr(String base, int n) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < n; i++) {
            res.append(base);
        }
        return res.toString();
    }

    public void testGenLiteralNull() {
        Assert.assertEquals("null", cg.genLiteralNull(sb()).toString());
    }

    public void testGenMethod() {
        // fail("Not yet implemented");
    }

    public void testGenMethodEnd() {
        // fail("Not yet implemented");
    }

    public void testGenMethodStart() {
        // fail("Not yet implemented");
    }

    public void testGenMultiLineComment() {
        assertEquals("/*\r\n * It's\r\n * multiline\r\n * comment\r\n */\r\n", cg.genMultiLineComment("It's\nmultiline\ncomment", sb()).toString());
    }

    public void testGenSingleLineComment() {
        assertEquals(" // It's single line comment\r\n", cg.genSingleLineComment("It's single line comment", sb()).toString());
    }

    public void testGenClassEnd() {
        // fail("Not yet implemented");
    }

    public void testGenClassStart() {
        // fail("Not yet implemented");
    }

    public void testGenModuleEnd() {
        // fail("Not yet implemented");
    }

    public void testGenModuleStart() {
        assertEquals("package org.openl.gen;\r\n", cg.genModuleStart(null, sb()).toString());
    }

    public void testGenBeanAttribute() {
        // fail("Not yet implemented");
    }

    public void testGenEscapedChar() {
        Assert.assertEquals("a", cg.genEscapedChar('a', sb()).toString());
        Assert.assertEquals("\\n", cg.genEscapedChar('\n', sb()).toString());
        Assert.assertEquals("\\033", cg.genEscapedChar('\033', sb()).toString());
        Assert.assertEquals("\\uab7c", cg.genEscapedChar('\uab7c', sb()).toString());
        Assert.assertEquals("\\b", cg.genEscapedChar('\b', sb()).toString());
        Assert.assertEquals("\\t", cg.genEscapedChar('\t', sb()).toString());
        Assert.assertEquals("\\f", cg.genEscapedChar('\f', sb()).toString());
        Assert.assertEquals("\\r", cg.genEscapedChar('\r', sb()).toString());
        Assert.assertEquals("\\n", cg.genEscapedChar('\n', sb()).toString());
        Assert.assertEquals("\\\\", cg.genEscapedChar('\\', sb()).toString());
    }

    public void testGenAttribute() {
        // fail("Not yet implemented");
    }

    public void testGenLiteralBool() {
        Assert.assertEquals("true", cg.genLiteralBool(true, sb()).toString());
        Assert.assertEquals("false", cg.genLiteralBool(false, sb()).toString());
    }

    public void testGetJpackage() {
        assertEquals("org.openl.gen", cg.getJpackage());
    }

    public void testSetJpackage() {
        cg.setJpackage("org.openl.custom.gen");
        assertEquals("org.openl.custom.gen", cg.getJpackage());
    }

    public void testGenInitFixedSizeArrayVar() {
        assertEquals("arr = new Object[0];\r\n", cg.genInitFixedSizeArrayVar("arr", "Object", 0, sb()).toString());
    }

    public void testGenInitializeBeanArray() {
        assertEquals("cars = new Car[3];\r\ncars[0] = new Car();\r\ncars[1] = new Car();\r\ncars[1].setModel(\"BMW\");\r\ncars[2] = null;\r\n",
                cg.genInitializeBeanArray("cars", new Car[] {new Car(null, 2011), new Car("BMW", 2019), null}, Car.class, null, sb()));
    }

    public void testGenLiteralConstraints() {
        assertEquals("new org.openl.rules.table.constraints.Constraints(\"foo\")", cg.genLiteralConstraints(new Constraints("foo"), sb()).toString());
    }

    public void testGenLiteralSystemValuePolicy() {
        assertEquals("SystemValuePolicy.IF_BLANK_ONLY", cg.genLiteralSystemValuePolicy(TablePropertyDefinition.SystemValuePolicy.IF_BLANK_ONLY, sb()).toString());
    }

    public void testGenLiteralLevelInheritance() {
        assertEquals(" InheritanceLevel.FILE ", cg.genLiteralLevelInheritance(InheritanceLevel.FILE, sb()).toString());
    }

    public void testGenLiteralMatchingExpression() {
        assertEquals("new org.openl.rules.table.properties.expressions.match.MatchingExpression(\"contains(caProvince)\")", cg.genLiteralMatchingExpression(new MatchingExpression("contains(caProvince)"), sb()).toString());
    }

    public void testGenLiteralTableType() {
        assertEquals("XlsNodeTypes.WORKSHEET", cg.genLiteralTableType(XlsNodeTypes.WORKSHEET, sb()).toString());
    }

    public void testGenLiteralErrorSeverity() {
        assertEquals("Severity.ERROR", cg.genLiteralErrorSeverity(Severity.ERROR, sb()).toString());
    }

    public void testGenLiteralJavaOpenClass() {
        assertEquals("org.openl.types.java.JavaOpenClass.getOpenClass(char.class)",
                cg.genLiteralJavaOpenClass(JavaOpenClass.CHAR, sb()).toString());
        assertEquals("org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Object[].class)",
                cg.genLiteralJavaOpenClass(new JavaOpenClass(Object[].class), sb()).toString());
    }

    public void testGetGenLevel() {
        assertEquals(0, cg.getGenLevel());
    }

    public void testSetGenLevel() {
        cg.setGenLevel(1);
        assertEquals(1, cg.getGenLevel());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    JavaCodeGen cg = new JavaCodeGen();
    JavaCodeGenController ctr = new JavaCodeGenController();

    StringBuilder sb() {
        return new StringBuilder();
    }

    public static class Car {

        private String model;
        private final Integer year;

        public Car() {
            year = null;
        }

        public Car(String model, Integer year) {
            this.model = model;
            this.year = year;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getYear() {
            return year;
        }
    }

}
