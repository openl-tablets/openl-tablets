/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RangeWithBounds;

/**
 * @author snshor
 */


public class RangeNodeBinder extends ANodeBinder
{
    private Number getMin(Number number){
        if(number.getClass() == Double.class){
            return Double.NEGATIVE_INFINITY;
        }else if(number.getClass() == Long.class){
            return Long.MIN_VALUE;
        }else{
            return Integer.MIN_VALUE;
        }
    }
    
    private Number getMax(Number number){
        if(number.getClass() == Double.class){
            return Double.POSITIVE_INFINITY;
        }else if(number.getClass() == Long.class){
            return Long.MAX_VALUE;
        }else{
            return Integer.MAX_VALUE;
        }
    }

    private Number getMinimalIncrease(Number number){
        if(number.getClass() == Double.class){
            return number.doubleValue() + Math.abs(number.doubleValue() / 1e15);
        }else if(number.getClass() == Long.class){
            return number.longValue() + 1;
        }else{
            return number.intValue() + 1;
        }
    }

    private Number getMinimalDecrease(Number number){
        if(number.getClass() == Double.class){
            return number.doubleValue() - Math.abs(number.doubleValue() / 1e15);
        }else if(number.getClass() == Long.class){
            return number.longValue() - 1;
        }else{
            return number.intValue() - 1;
        }
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
	    throws Exception
    {
    	IBoundNode[] children = bindChildren(node, bindingContext);
    	
    	final String type = node.getType();
		Number val = (Number)((LiteralBoundNode)children[0]).getValue();

    	
    	if (type.contains("binary"))
    	{	
    	    Number val2 = (Number)((LiteralBoundNode)children[1]).getValue();
    		if (val.doubleValue() > val2.doubleValue())
    			throw new BoundError(node, val2 + " must be more or equal than " + val);
    		
    		if (type.endsWith("minus") || type.endsWith("ddot") )
            {
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val, val2), 
        						JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }
    		if (type.endsWith("tdot") )
            {
                return new LiteralBoundNode(node, 
                        new RangeWithBounds(getMinimalIncrease(val), getMinimalDecrease(val2)), 
                                JavaOpenClass.getOpenClass(RangeWithBounds.class));
            }
    	}	
    	
    	if (type.contains("number"))
    		return new LiteralBoundNode(node, 
    				new RangeWithBounds((Number)val,(Number)val), 
    						JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		

    	if (type.contains("unary.prefix"))
    	{	
    		if (type.endsWith("lt"))
    		{	
                return new LiteralBoundNode(node, 
                        new RangeWithBounds(getMin(val), getMinimalDecrease(val)), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		else if(type.endsWith("le"))
    		{	
                return new LiteralBoundNode(node, 
                        new RangeWithBounds(getMin(val), val), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}  
    		else if(type.endsWith("gt"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(getMinimalIncrease(val), getMax(val)), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		} 
    		else if(type.endsWith("ge"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val , getMax(val)), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		throw new RuntimeException("Unsupported range prefix type: " + type);
    	}	
    
    	if (type.contains("unary.suffix"))
    	{
    		
    		if (type.endsWith("lt"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(getMinimalIncrease(val), getMax(val)), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}
    		else if(type.endsWith("le") || type.endsWith("plus"))
    		{	
        		return new LiteralBoundNode(node, 
        				new RangeWithBounds(val, getMax(val)), JavaOpenClass.getOpenClass(RangeWithBounds.class));
    		}  
    		throw new RuntimeException("Unsupported range suffix type: " + type);
    	}	
    		
    	
    	
    	
		throw new RuntimeException("Unsupported range type: " + type);
    	
    	
    }




}
