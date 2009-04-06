/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax.impl;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.IGrammar;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;


/**
 * @author snshor
 *
 */
public abstract class Grammar implements IGrammar
{
	protected SyntaxTreeBuilder stb = new SyntaxTreeBuilder();
	
	
	
	
	
  /**
   * @return
   */
  public SyntaxTreeBuilder getSyntaxTreeBuilder()
  {
    return stb;
  }
  /* (non-Javadoc)
   * @see org.openl.syntax.IGrammar#getErrors()
   */
  public ISyntaxError[] getErrors()
  {
    return stb.getSyntaxErrors();
  }

  /* (non-Javadoc)
   * @see org.openl.syntax.IGrammar#getTopNode()
   */
  public ISyntaxNode getTopNode()
  {
    return stb.getTopnode();
  }

  /* (non-Javadoc)
   * @see org.openl.syntax.IGrammar#setModule(org.openl.IOpenSourceCodeModule)
   */
  public void setModule(IOpenSourceCodeModule module)
  {
  	stb.setModule(module);
  }

}
