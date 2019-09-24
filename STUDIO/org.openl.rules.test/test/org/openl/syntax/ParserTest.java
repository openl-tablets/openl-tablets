/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.openl.OpenL;
import org.openl.binding.INodeBinder;
import org.openl.binding.impl.DoubleNodeBinder;
import org.openl.binding.impl.IntNodeBinder;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.conf.OpenLConfigurationException;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.NaryNode;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class ParserTest extends TestCase {

    private static ISyntaxNode search(ISyntaxNode topNode, String type) {
        Class<?> c = null;
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

    /**
     * Constructor for ParserTest.
     *
     * @param arg0
     */
    public ParserTest(String arg0) {
        super(arg0);
    }

    public void _testLiteral(String src, String res, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        ISyntaxNode ln = search(pc.getTopNode(), type);

        Assert.assertEquals(res, ln.getText());
        Assert.assertEquals(type, ln.getType());
    }

    public void _testMethodHeader(String src, String res, String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodHeader(new StringSourceCodeModule(src, null));

        Assert.assertEquals(0, pc.getErrors().length);

        ISyntaxNode syntaxNode = pc.getTopNode();

        Assert.assertEquals(type, syntaxNode.getType());
    }

    public void _testModule(String src, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsModule(new StringSourceCodeModule(src, null));

        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        ISyntaxNode bn = search(pc.getTopNode(), type);

        Assert.assertNotNull(bn);

        Assert.assertEquals(type, bn.getType());
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
            throw new RuntimeException("This expression must produce an error!");
        }

        String message = errors[0].getMessage();

        if (!message.startsWith(messageStart)) {
            throw new RuntimeException(String.format("'%s' should start with '%s'", message, messageStart));
        }

    }

    public void _testType(String src, final String type) throws OpenLConfigurationException {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        ISyntaxNode bn = search(pc.getTopNode(), type);
        Assert.assertNotNull(bn);

        Assert.assertEquals(type, bn.getType());
    }

    public void testOfMethod() {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.of(2019, 1, 1)", null));

        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("policy.of == policy.the", null));

        error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

    }

    public void testOperatorMethods() {
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);

        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.or()", null));
        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.not()", null));
        error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("LocalDate.and()", null));
        error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule("a.not == (a.or == a.and)", null));
        error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

    }

    public void _testNumberParseAndBind(INodeBinder binder,
            String src,
            Object res,
            Class<?> clazz,
            final String type) throws Exception {

        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        ISyntaxNode ln = search(pc.getTopNode(), type);

        LiteralBoundNode literalBoundNode = (LiteralBoundNode) binder.bind(ln, null);
        Assert.assertEquals(clazz, literalBoundNode.getType().getInstanceClass());
        Assert.assertEquals(res, literalBoundNode.getValue());
    }

    public void testArray() {
        _testType("new int[10]", "op.new.array");
        _testType("new int[10][]", "op.new.array");
        _testType("new int[10][20][]", "op.new.array");
    }

    public void testAssign() {
        _testType("int x = y; z", "local.var.declaration");
    }

    public void testChain() {
        _testType("x.y", "chain");
        _testType("x.y[10].foo(z)", "chain");
        _testType("x.y", "chain");
    }

    public void testFunc() throws OpenLConfigurationException {
        _testType("sin(5, 10)", "function");
    }

    public void testErr1() throws OpenLConfigurationException {

        _testErrorMsg("sin(5, 10", "Need to close '('");
    }

    public void testErr2() throws OpenLConfigurationException {
        _testErrorMsg("\"abc", "Lexical error at line");
    }

    public void testErr3() throws OpenLConfigurationException {
        _testErrorMsg("x=y{y=z}", "Encountered");
    }

    public void testErr4() throws OpenLConfigurationException {
        _testErrorMsg("return u", "Encountered");
    }

    public void testErr5() throws OpenLConfigurationException {
        _testErrorMsg("\"ab\\zc\"", "Lexical error at line");
    }

    public void testIf() {
        _testType("if (x) a();", "control.if");
    }

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

    public void testRange() throws OpenLConfigurationException {
        _testType("$Step1:$Step7", "range.variable");
    }

    public void testLocation() throws OpenLConfigurationException {
        String test1 = "\tx";
        OpenL op = OpenL.getInstance(OpenL.OPENL_J_NAME);
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(test1, null));
        ILocation loc = pc.getTopNode().getSourceLocation();
        Assert.assertEquals(1, loc.getStart().getAbsolutePosition(new TextInfo(test1)));

    }

    public void testMethod() {
        _testModule("int x(){return 5;}", "module.top");
    }

    public void testMethodHeader() {
        _testMethodHeader("int x(a a1, b b1)", null, "method.header");
    }

    public void testOperator() throws OpenLConfigurationException {
        BinaryNode binaryNode = _testOperator("x+y", "op.binary.add");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x-3", "op.binary.subtract");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x%3", "op.binary.rem");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x is less than 3", "op.binary.lt");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x is  less  than 3", "op.binary.lt");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x or y", "op.binary.or");
        Assert.assertNotNull(binaryNode);

        binaryNode = _testOperator("x and y", "op.binary.and");
        Assert.assertNotNull(binaryNode);

        NaryNode naryNode = _testOperator("x?y: z", "op.ternary.qmark");
        Assert.assertNotNull(naryNode);
    }

    public void testParse() {

        // new Parser().parse("y - (x.z + t); x+y", null, new JGrammar());

    }

    public void testNumberParseAndBind() throws Exception {
        _testNumberParseAndBind(new IntNodeBinder(), "1000000", 1000000, int.class, "literal.integer");
        _testNumberParseAndBind(new IntNodeBinder(), "1000000000000", 1000000000000L, long.class, "literal.integer");
        _testNumberParseAndBind(new IntNodeBinder(),
            "10000000000000000000",
            new BigInteger("10000000000000000000"),
            BigInteger.class,
            "literal.integer");

        _testNumberParseAndBind(new DoubleNodeBinder(),
            "1e+308",
            Double.valueOf("1e+308"),
            double.class,
            "literal.real");
        _testNumberParseAndBind(new DoubleNodeBinder(),
            "2e+308",
            new BigDecimal("2e+308"),
            BigDecimal.class,
            "literal.real");
    }

}
