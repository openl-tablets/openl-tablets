/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax.impl;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;



/**
 * @author snshor
 *
 */
public class ParsedCode implements IParsedCode

{
	
	ISyntaxNode topnode;
	ISyntaxError[] syntaxErrors;
	IOpenSourceCodeModule source;	

	
	public ParsedCode(ISyntaxNode topnode, IOpenSourceCodeModule source, 	ISyntaxError[] syntaxErrors)
	{
		this.topnode	= topnode;
		this.syntaxErrors = syntaxErrors;
		this.source = source;	
	}	
	

 
  /* (non-Javadoc)
   * @see org.openl.syntax.IParsedCode#getError()
   */
  public ISyntaxError[] getError()
  {
    return syntaxErrors;
  }

  /* (non-Javadoc)
   * @see org.openl.syntax.IParsedCode#getSource()
   */
  public IOpenSourceCodeModule getSource()
  {
    return source;
  }

  /* (non-Javadoc)
   * @see org.openl.syntax.IParsedCode#getTopNode()
   */
  public ISyntaxNode getTopNode()
  {
    return topnode;
  }

}
