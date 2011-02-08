/*
 * Created on May 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.conf.OpenConfigurationException;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ASyntaxNode;
import org.openl.syntax.impl.BinaryNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;
import org.openl.util.tree.TreeIterator;

/**
 * @author snshor
 *
 */
public class ParserTest extends TestCase {

    public static void main(String[] args) {

        String src = " x < y + 10";
        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));
        System.out.println(pc.getTopNode());

    }

    /**
     * Constructor for ParserTest.
     *
     * @param arg0
     */
    public ParserTest(String arg0) {
        super(arg0);
    }

    @SuppressWarnings("unchecked")
    public void _testLiteral(String src, String res, String type) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        TreeIterator it = new TreeIterator(pc.getTopNode(), ASyntaxNode.TREE_ADAPTOR, TreeIterator.DEFAULT);

        LiteralNode ln = (LiteralNode) it.select(ASelector.selectClass(LiteralNode.class)).next();

        Assert.assertEquals(res, ln.getImage());
        Assert.assertEquals(type, ln.getType());
    }

    public void _testMethodHeader(String src, String res, String type) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsMethodHeader(new StringSourceCodeModule(src, null));

        Assert.assertEquals(0, pc.getErrors().length);

        ISyntaxNode syntaxNode = pc.getTopNode();

        Assert.assertEquals(type, syntaxNode.getType());
    }

    @SuppressWarnings("unchecked")
    public void _testModule(String src, final String type) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsModule(new StringSourceCodeModule(src, null));

        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        TreeIterator it = new TreeIterator(pc.getTopNode(), ASyntaxNode.TREE_ADAPTOR, TreeIterator.DEFAULT);

        ISelector sel = new ASelector() {
            public boolean select(Object obj) {
                return ((ISyntaxNode) obj).getType().equals(type);
            }
        };

        ISyntaxNode bn = (ISyntaxNode) it.select(sel).next();
        Assert.assertNotNull(bn);

        Assert.assertEquals(type, bn.getType());
    }

    @SuppressWarnings("unchecked")
    public void _testOperator(String src, String type) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        TreeIterator it = new TreeIterator(pc.getTopNode(), ASyntaxNode.TREE_ADAPTOR, TreeIterator.DEFAULT);

        BinaryNode bn = (BinaryNode) it.select(ASelector.selectClass(BinaryNode.class)).next();

        Assert.assertEquals(type, bn.getType());
    }

    @SuppressWarnings("unchecked")
    public void _testType(String src, final String type) throws OpenConfigurationException {

        OpenL op = OpenL.getInstance("org.openl.j");
        IParsedCode pc = op.getParser().parseAsMethodBody(new StringSourceCodeModule(src, null));

        SyntaxNodeException[] error = pc.getErrors();
        if (error.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", error);
        }

        TreeIterator it = new TreeIterator(pc.getTopNode(), ASyntaxNode.TREE_ADAPTOR, TreeIterator.DEFAULT);

        ISelector sel = new ASelector() {
            public boolean select(Object obj) {
                return ((ISyntaxNode) obj).getType().equals(type);
            }
        };

        ISyntaxNode bn = (ISyntaxNode) it.select(sel).next();
        Assert.assertNotNull(bn);

        Assert.assertEquals(type, bn.getType());
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

    public void testFunc() throws OpenConfigurationException {
        _testType("sin(5, 10)", "function");
    }

    public void testIf() {
        _testType("if (x) a();", "control.if");
    }

    public void testLiteral() throws OpenConfigurationException {
        // we should remove suffix the next line produces NumberFormatException
        // Assert.assertEquals(new Long(5), Long.decode("5L"));

        // _testLiteral("-5L", "-5L", "literal.integer");
        _testLiteral("0xff", "0xff", "literal.integer");
        _testLiteral("5L", "5L", "literal.integer");
        _testLiteral("\"ab\\n\"", "\"ab\\n\"", "literal.string");

    }

    public void testLocation() throws OpenConfigurationException {
        String test1 = "\tx";
        OpenL op = OpenL.getInstance("org.openl.j");
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

    public void testOperator() throws OpenConfigurationException {
        _testOperator("x+y", "op.binary.add");
        _testOperator("x+3", "op.binary.add");
        _testOperator("x-3", "op.binary.subtract");
        _testOperator("x%3", "op.binary.rem");        
    }

    public void testParse() {

        // new Parser().parse("y - (x.z + t); x+y", null, new JGrammar());

    }

}
