/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import org.openl.OpenL;
import org.openl.binding.INodeBinder;
import org.openl.binding.impl.CharNodeBinder;
import org.openl.binding.impl.DoubleNodeBinder;
import org.openl.binding.impl.IntNodeBinder;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.StringNodeBinder;
import org.openl.conf.OpenLConfigurationException;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.NaryNode;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * @author snshor
 */
public class ParserTest {

    private static ISyntaxNode search(ISyntaxNode topNode, String type) {
        if (topNode.getType().equals(type)) {
            return topNode;
        }
        int children = topNode.getNumberOfChildren();
        for (int i = 0; i < children; i++) {
            ISyntaxNode child = search(topNode.getChild(i), type);
            if (child != null) {
                return child;
            }

        }
        return null;

    }

    public void _testLiteral(String src, String res, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        ISyntaxNode ln = search(pc.getTopNode(), type);

        assertEquals(res, ln.getText());
        assertEquals(type, ln.getType());
    }

    public void _testMethodHeader(String src, String res, String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodHeader(new StringSourceCodeModule(src, null));

        assertEquals(0, pc.getErrors().length);

        ISyntaxNode syntaxNode = pc.getTopNode();

        assertEquals(type, syntaxNode.getType());
    }

    @SuppressWarnings("unchecked")
    public <T extends ISyntaxNode> T _testOperator(String src, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));
        return (T) search(pc.getTopNode(), type);
    }

    public void _testErrorMsg(String src, String messageStart) {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        SyntaxNodeException[] errors = pc.getErrors();

        if (errors.length == 0) {
            throw new RuntimeException("This expression must produce an error.");
        }

        String message = errors[0].getMessage();

        if (!message.startsWith(messageStart)) {
            throw new RuntimeException(String.format("'%s' should start with '%s'", message, messageStart));
        }

    }

    public void _testType(String src, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

        ISyntaxNode bn = search(pc.getTopNode(), type);
        assertNotNull(bn);

        assertEquals(type, bn.getType());
    }

    @Test
    public void testOfMethod() {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.of(2019, 1, 1)", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("policy.of == policy.the", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());
    }

    @Test
    public void testOperatorMethods() {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);

        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.or()", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.not()", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.and()", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("a.not == (a.or == a.and)", null));
        assertArrayEquals(SyntaxNodeException.EMPTY_ARRAY, pc.getErrors());

    }

    public void _testLiteralParseAndBind(INodeBinder binder,
                                         String src,
                                         Object res,
                                         Class<?> clazz,
                                         final String type) throws Exception {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));
        ISyntaxNode ln = search(pc.getTopNode(), type);

        LiteralBoundNode literalBoundNode = (LiteralBoundNode) binder.bind(ln, null);
        assertEquals(clazz, literalBoundNode.getType().getInstanceClass());
        assertEquals(res, literalBoundNode.getValue());
    }

    @Test
    public void testArray() {
        _testType("new int[10]", "op.new.array");
        _testType("new int[10][]", "op.new.array");
        _testType("new int[10][20][]", "op.new.array");
    }

    @Test
    public void testAssign() {
        _testType("int x = y; z", "local.var.declaration");
    }

    @Test
    public void testChain() {
        _testType("x.y", "chain");
        _testType("x.y[10].foo(z)", "chain");
        _testType("x.y", "chain");
    }

    @Test
    public void testFunc() throws OpenLConfigurationException {
        _testType("sin(5, 10)", "function");
    }

    @Test
    public void testErr1() throws OpenLConfigurationException {
        _testErrorMsg("sin(5, 10", "Need to close '('");
    }

    @Test
    public void testErr2() throws OpenLConfigurationException {
        _testErrorMsg("\"abc", "Lexical error at line");
    }

    @Test
    public void testErr3() throws OpenLConfigurationException {
        _testErrorMsg("x=y{y=z}", "Encountered");
    }

    @Test
    public void testErr4() throws OpenLConfigurationException {
        _testErrorMsg("return u", "Encountered");
    }

    @Test
    public void testErr5() throws OpenLConfigurationException {
        _testErrorMsg("\"ab\\zc\"", "Lexical error at line");
    }

    @Test
    public void testIf() {
        _testType("if (x) a();", "control.if");
    }

    @Test
    public void testLiteral() throws OpenLConfigurationException {
        // we should remove suffix the next line produces NumberFormatException
        // Assert.assertEquals(new Long(5), Long.decode("5L"));

        // _testLiteral("-5L", "-5L", "literal.integer");
        _testLiteral("0xff", "0xff", "literal.integer");
        _testLiteral("5L", "5L", "literal.integer");
        _testLiteral("\"ab\\n\"", "\"ab\\n\"", "literal.string");
        _testLiteral("2001-01-01", "2001-01-01", "literal.date");
        _testLiteral("11:40", "11:40", "literal.time");
        _testLiteral("11:40:33", "11:40:33", "literal.time");
        _testLiteral("11:40:33.744", "11:40:33.744", "literal.time");
        _testLiteral("2001-01-01 11:40:33.744", "2001-01-01 11:40:33.744", "literal.datetime");

    }

    @Test
    public void testRange() throws OpenLConfigurationException {
        _testType("$Step1:$Step7", "range.variable");
    }

    @Test
    public void testLocation() throws OpenLConfigurationException {
        String test1 = "\tx";
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(test1, null));
        ILocation loc = pc.getTopNode().getSourceLocation();
        assertEquals(1, loc.getStart().getAbsolutePosition(new TextInfo(test1)));

    }

    @Test
    public void testMethodHeader() {
        _testMethodHeader("int x(a a1, b b1)", null, "method.header");
    }

    @Test
    public void testOperator() throws OpenLConfigurationException {
        BinaryNode binaryNode = _testOperator("x+y", "op.binary.add");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x-3", "op.binary.subtract");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x%3", "op.binary.rem");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x is less than 3", "op.binary.lt");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x is  less  than 3", "op.binary.lt");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x or y", "op.binary.or");
        assertNotNull(binaryNode);

        binaryNode = _testOperator("x and y", "op.binary.and");
        assertNotNull(binaryNode);

        NaryNode naryNode = _testOperator("x?y: z", "op.ternary.qmark");
        assertNotNull(naryNode);

        naryNode = _testOperator("x?y:z", "op.ternary.qmark");
        assertNotNull(naryNode);
    }

    @Test
    public void testNumberParseAndBind() throws Exception {
        _testLiteralParseAndBind(new IntNodeBinder(), "1000000", 1000000, int.class, "literal.integer");
        _testLiteralParseAndBind(new IntNodeBinder(), "1000000000000", 1000000000000L, long.class, "literal.integer");
        _testLiteralParseAndBind(new IntNodeBinder(),
                "10000000000000000000",
                new BigInteger("10000000000000000000"),
                BigInteger.class,
                "literal.integer");

        _testLiteralParseAndBind(new DoubleNodeBinder(),
                "1e+308",
                Double.valueOf("1e+308"),
                double.class,
                "literal.real");
        _testLiteralParseAndBind(new DoubleNodeBinder(),
                "2e+308",
                new BigDecimal("2e+308"),
                BigDecimal.class,
                "literal.real");
    }

    @Test
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public void testStringParseAndBind() throws Exception {
        _testLiteralParseAndBind(new StringNodeBinder(),
                wrapStrLit("\\u00A0"),
                "\u00A0",
                String.class,
                "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(),
                wrapStrLit("\\uAAAA"),
                "\uAAAA",
                String.class,
                "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(),
                wrapStrLit("\\u222b"),
                "\u222b",
                String.class,
                "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(),
                wrapStrLit("\\u00BD"),
                "\u00BD",
                String.class,
                "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\123bla"), "\123bla", String.class, "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\1230bla"), "\1230bla", String.class, "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\128bla"), "\128bla", String.class, "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\78bla"), "\78bla", String.class, "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\33338bla"), "\33338bla", String.class, "literal.string");
        _testLiteralParseAndBind(new StringNodeBinder(), wrapStrLit("\\7"), "\7", String.class, "literal.string");

        _testLiteralParseAndBind(new StringNodeBinder(),
                wrapStrLit("H\\u00e5ll\\u00F8, W\\u00f8\\u00AEl\\u2202\\u00a1"),
                "H\u00e5ll\u00F8, W\u00f8\u00AEl\u2202\u00a1",
                String.class,
                "literal.string");
    }

    @Test
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public void testCharParseAndBind() throws Exception {
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\u00A0"), '\u00A0', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\uAAAA"), '\uAAAA', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\u222b"), '\u222b', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\u00BD"), '\u00BD', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\123"), '\123', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\12"), '\12', char.class, "literal.char");
        _testLiteralParseAndBind(new CharNodeBinder(), wrapChLit("\\1"), '\1', char.class, "literal.char");
    }

    private static String wrapChLit(String s) {
        return '\'' + s + '\'';
    }

    private static String wrapStrLit(String s) {
        return '\"' + s + '\"';
    }

}
