/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodHeader;
import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodHeaderNode extends ABoundNode implements IBoundMethodHeader
{

	IOpenMethodHeader methodHeader;

  /**
   * @param syntaxNode
   * @param children
   */
  public MethodHeaderNode(ISyntaxNode syntaxNode, IOpenMethodHeader methodHeader)
  {
    super(syntaxNode, IBoundNode.EMPTY);
    this.methodHeader = methodHeader;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundMethodHeader#getMethodHeader()
   */
  public IOpenMethodHeader getMethodHeader()
  {
    return methodHeader;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getType()
   */
  public IOpenClass getType()
  {
    return methodHeader.getType();
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#evaluateRuntime(org.openl.vm.IRuntimeEnv)
   */
  public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
  {
    throw new UnsupportedOperationException();
  }

	public void updateDependency(BindingDependencies dependencies)
	{
		
	}
  
  
  

}
