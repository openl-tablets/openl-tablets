/*
 * Created on Jul 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax;

import java.io.Reader;

import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public interface IGrammar
{
	public void setModule(IOpenSourceCodeModule module);

	public void parseAsModule(Reader r);
	
	public void parseAsMethod(Reader r);

	public void parseAsMethodHeader(Reader r);
	
	public ISyntaxNode getTopNode();

	public ISyntaxError[] getErrors();

	/**
	 * @param reader
	 */
	public void parseAsType(Reader reader);

}
