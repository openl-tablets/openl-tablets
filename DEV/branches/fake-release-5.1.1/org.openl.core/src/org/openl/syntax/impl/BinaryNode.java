/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax.impl;

import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 *
 */
public class BinaryNode extends ASyntaxNode
{
	ISyntaxNode left; 
	ISyntaxNode right;
	
	public BinaryNode(String type, TextInterval pos, ISyntaxNode left, ISyntaxNode right, Map properties, IOpenSourceCodeModule module)
	{
		
		super(type, pos, properties, module);
		this.left = left;
		this.right = right;
	}			
	
	public ISyntaxNode getChild(int i)
	{
		if (i == 0)
			return left;
		if (i == 1)
			return right;
		throw new RuntimeException("BinaryNode has only two children, not " + (i+1)); 		
	}
	
	public int getNumberOfChildren()
	{
		return 2;
	}
	



}
