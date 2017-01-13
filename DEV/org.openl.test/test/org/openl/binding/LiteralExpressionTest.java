package org.openl.binding;

import junit.framework.TestCase;

import org.junit.Assert;
import org.openl.IOpenVM;
import org.openl.OpenL;
import org.openl.engine.OpenLSourceManager;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.ProcessedCode;


public class LiteralExpressionTest  extends TestCase{

    
    protected ProcessedCode compile(String expr, OpenL openl)
    {
        OpenLSourceManager sm = new OpenLSourceManager(openl);
        
        return sm.processSource(new StringSourceCodeModule(expr, null), SourceType.METHOD_BODY);
    }
    
    
    protected Object evaluate(ProcessedCode pc, OpenL openl)
    {        
        
        IOpenVM vm = openl.getVm();

        return vm.getRunner().run((IBoundMethodNode)pc.getBoundCode().getTopNode(), new Object[0]);
        
    }
    
    public void _testExpr(String expr, boolean literal, Object expected)
    {
        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        ProcessedCode pc = compile(expr, openl);
        
        IBoundNode top = pc.getBoundCode().getTopNode();
        
        Assert.assertEquals(literal, top.isLiteralExpression());
    
        if (literal)
        {
            Object res = evaluate(pc, openl);
            Assert.assertEquals(expected, res);
        }    
    }
    
    
    public void testLiteral()
    {
        _testExpr("9", true, 9);
        _testExpr("9+7", true, 16);
        _testExpr("int x = 5; 9+x", false, 16);
        _testExpr("Math.sin(0)", true, 0.0);
        _testExpr("Math.cos(0)", true, 1.0);
        _testExpr("System.out.println(0)", false, 1.0);
    }
    
    
    
    
    
    
}
