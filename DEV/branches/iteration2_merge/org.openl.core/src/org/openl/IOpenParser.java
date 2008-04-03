/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl;

import org.openl.syntax.IParsedCode;



/**
 * @author snshor
 *
 */
public interface IOpenParser
{
	
	IParsedCode parseAsMethodHeader(IOpenSourceCodeModule m);
	
	IParsedCode parseAsMethodBody(IOpenSourceCodeModule m);

	IParsedCode parseAsModule(IOpenSourceCodeModule m);

	/**
	 * @param src
	 * @return
	 */
	IParsedCode parseAsType(IOpenSourceCodeModule src); 
	
//	IParsedCode parseAsMethod(String code);
//
//	IParsedCode parseAsModule(String code);
	
}
