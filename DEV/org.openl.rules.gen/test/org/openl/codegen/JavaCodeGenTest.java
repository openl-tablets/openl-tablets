package org.openl.codegen;

import org.junit.Assert;

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
        // fail("Not yet implemented");
    }

    public void testGenSingleLineComment() {
        // fail("Not yet implemented");
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
        // fail("Not yet implemented");
    }

    public void testGenBeanAttribute() {
        // fail("Not yet implemented");
    }

    public void testGenEscapedChar() {
        Assert.assertEquals("a", cg.genEscapedChar('a', sb()).toString());
        Assert.assertEquals("\\n", cg.genEscapedChar('\n', sb()).toString());
        Assert.assertEquals("\\033", cg.genEscapedChar('\033', sb()).toString());
        Assert.assertEquals("\\uab7c", cg.genEscapedChar('\uab7c', sb()).toString());
    }

    public void testGenAttribute() {
        // fail("Not yet implemented");
    }

    public void testGenLiteralBool() {
        Assert.assertEquals("true", cg.genLiteralBool(true, sb()).toString());
        Assert.assertEquals("false", cg.genLiteralBool(false, sb()).toString());
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

}
