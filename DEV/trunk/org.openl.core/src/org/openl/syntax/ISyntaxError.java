/*
 * Created on May 30, 2003
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
public interface ISyntaxError
{
	public static final ISyntaxError[] EMPTY = {};

  public ILocation getLocation();
  public String getMessage();
  public ISyntaxNode getSyntaxNode();
  
  
  public IOpenSourceCodeModule getModule();
  
  public Throwable getThrowable();
  
  // to improve navigation
  public ISyntaxNode getTopLevelSyntaxNode();
  public void setTopLevelSyntaxNode(ISyntaxNode topLevelNode);
  

}
