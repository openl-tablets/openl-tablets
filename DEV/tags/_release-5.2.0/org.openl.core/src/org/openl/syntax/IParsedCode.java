/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax;

import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public interface IParsedCode
{
	IOpenSourceCodeModule getSource();
	ISyntaxNode getTopNode();

	ISyntaxError[] getError();	
			
}
