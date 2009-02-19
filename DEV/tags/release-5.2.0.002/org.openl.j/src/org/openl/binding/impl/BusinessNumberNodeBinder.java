/*
 * Created on May 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.LiteralNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class BusinessNumberNodeBinder extends ANodeBinder
{
	

  public IBoundNode bind(
    ISyntaxNode node,
    IBindingContext bindingContext) throws SyntaxError
  {
  	String s = ((LiteralNode)node).getImage();
  	
  	if (s.charAt(0) == '$')
  		 s = s.substring(1);
  	
  	int len = s.length();
  	
  	char last = Character.toUpperCase(s.charAt(len - 1));
  	
  	String s1 = s.substring(0, len-1);
  	

  	switch(last)
  	{
  		case 'K':
  			return makeNum(s1, 1000, node);
  		case 'M':	
  			return makeNum(s1,  1000* 1000, node);
  		case 'B':	
  			return makeNum(s1, 1000 * 1000 * 1000, node);
  		default:
  			return makeNum(s, 1, node);
  			
  	}
		  	
  }

private static IBoundNode makeNum(String s, int mul, ISyntaxNode node) throws SyntaxError 
{
	if (s.indexOf(',') >= 0)
	{
		s = s.replace(",", "");
	}	
	
	if (s.indexOf('.') >= 0)
	{
		Double x = Double.parseDouble(s) * mul;
		if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE)
//			return new LiteralBoundNode(node, x.longValue(), JavaOpenClass.LONG);
			throw new SyntaxError(node, "Number " + x.longValue() + " is outside the valid range", null);
		return new LiteralBoundNode(node, Math.round(x), JavaOpenClass.INT);
	}	
	
	Long x = Long.parseLong(s) * mul;
	if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE)
//		return new LiteralBoundNode(node, x, JavaOpenClass.LONG);
		throw new SyntaxError(node, "Number " + x.longValue() + " is outside the valid range", null);

	return new LiteralBoundNode(node, x.intValue(), JavaOpenClass.INT);
}

}
