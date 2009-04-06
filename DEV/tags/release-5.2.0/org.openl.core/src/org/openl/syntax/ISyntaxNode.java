/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax;

import org.openl.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public interface ISyntaxNode
{
	
	IOpenSourceCodeModule getModule();

	public int getNumberOfChildren();
	
	public ISyntaxNode getChild(int i);
	
	public String getType();
	
	public ILocation getSourceLocation();
	
	
	void setParent(ISyntaxNode node);
	ISyntaxNode getParent();
	
//	public Map<String, String> getProperties();

	/**
	 * @param i
	 * @param buf
	 */
	void print(int i, StringBuffer buf);	
	
//	public String getNamespace();
	
}

