/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.ABoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ParameterNode extends ABoundNode
{

	protected String name;
	protected IOpenClass type;

  /**
   * @param syntaxNode
   * @param children
   */
  public ParameterNode(ISyntaxNode syntaxNode, String name, IOpenClass type)
  {
    super(syntaxNode, IBoundNode.EMPTY);
    this.name = name;
    this.type = type;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
   */
	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
  {
  	throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getType()
   */
  public IOpenClass getType()
  {
    return type;
  }

  /**
   * @return
   */
  public String getName()
  {
    return name;
  }

	public void updateDependency(BindingDependencies dependencies)
	{
		dependencies.addTypeDependency(type, this);
	}

}
