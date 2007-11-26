package org.openl.grammars.bex;

import java.io.StringReader;

import org.openl.IOpenSourceCodeModule;
import org.openl.grammars.constexpr.BExGrammar;
import org.openl.syntax.IGrammar;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.StringSourceCodeModule;

import junit.framework.TestCase;

public class BExGrammarTest extends TestCase
{

    public void testG1()
    {

	_testG1(" $500.23 less than $0.33K more or equal agreed value / market value");
	_testG1(" int x = 10; long y;" +
			"Agreed Value  > 120% of the market value ");
    }
    
    
    void _testG1(String src)
    {
	IOpenSourceCodeModule module = new StringSourceCodeModule(src, null);
	IGrammar g = new BExGrammar();
	g.setModule(module);
	g.parseAsMethod(module.getCharacterStream());

	ISyntaxError[] err = g.getErrors();
	for (int i = 0; i < err.length; i++)
	{
	    System.out.println(err[i]);
	}
	System.out.println(g.getTopNode());
	
    }
}
