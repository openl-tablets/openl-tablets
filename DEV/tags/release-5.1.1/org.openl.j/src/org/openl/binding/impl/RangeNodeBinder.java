/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RangeWithBounds;

/**
 * @author snshor
 */

public class RangeNodeBinder extends ANodeBinder
{

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
	    throws Exception
    {
    	IBoundNode[] children = bindChildren(node, bindingContext);
    	
    	final String type = node.getType();
		Number x = (Number)((LiteralBoundNode)children[0]).getValue();
		int val = x.intValue();

    	
    	if (type.contains("binary"))
    		return new LiteralBoundNode(node, 
    				new RangeWithBounds((Number)val,(Number)((LiteralBoundNode)children[1]).getValue()), 
    						JavaOpenClass.getOpenClass(RangeWithBounds.class));

    	if (type.contains("unary.prefix"))
    	{	
    		if (type.endsWith("lt"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(Integer.MIN_VALUE, val-1), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		else if(type.endsWith("le"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(Integer.MIN_VALUE, val), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}  
    		else if(type.endsWith("gt"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val + 1, Integer.MAX_VALUE), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		} 
    		else if(type.endsWith("ge"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val , Integer.MAX_VALUE), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		throw new RuntimeException("Unsupported range prefix type: " + type);
    	}	
    
    	if (type.contains("unary.suffix"))
    	{
    		
    		if (type.endsWith("lt"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val+1, Integer.MAX_VALUE), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		else if(type.endsWith("le") || type.endsWith("plus"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val, Integer.MAX_VALUE), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}  
    		throw new RuntimeException("Unsupported range suffix type: " + type);
    	}	
    		
    	
    	
    	
		throw new RuntimeException("Unsupported range type: " + type);
    	
    	
    }




}
