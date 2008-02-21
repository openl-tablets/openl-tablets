/*
 * Created on May 13, 2003
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
public class NaryNode extends ASyntaxNode
{

	protected ISyntaxNode[] nodes;

	public NaryNode(String type, TextInterval pos, ISyntaxNode[] nodes, Map properties, IOpenSourceCodeModule module)
	{
		super(type, pos, properties, module);
		this.nodes = nodes;
		
	}


  /* (non-Javadoc)
   * @see org.openl.parser.SyntaxNode#getNumberOfChildren()
   */
  public int getNumberOfChildren()
  {
    return nodes.length;
  }

  /* (non-Javadoc)
   * @see org.openl.parser.SyntaxNode#getChild(int)
   */
  public ISyntaxNode getChild(int i)
  {
    return nodes[i];
  }


	public ISyntaxNode[] getNodes()
	{
		return nodes;
	}


	public void setNodes(ISyntaxNode[] nodes)
	{
		this.nodes = nodes;
	}

}
