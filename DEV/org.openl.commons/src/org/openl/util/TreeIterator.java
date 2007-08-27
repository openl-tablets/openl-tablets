/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.util.Iterator;
import java.util.Stack;

/**
 * @author snshor
 *
 */
public class TreeIterator extends AOpenIterator
{
	
	Object currentNode;
	TreeAdaptor adaptor;
	Stack path = new Stack(); 
	int mode = 0;
	Iterator children = null;	
	
	static final public int
	  DEFAULT = 0, // top-bottom, left-right, all nodes included
	  LEAVES_ONLY = 1, 
	  RIGHT_TO_LEFT = 2, 
	  NO_LEAVES = 4,  
	  BOTTOM_TOP = 8; // iterate over children first  
	
	
	public TreeIterator(Object treeRoot, TreeAdaptor adaptor, int mode)
	{
		this.children = single(treeRoot); 
		this.adaptor = adaptor;
		this.mode = mode;
		findNextNode();
	}
	
	public static interface TreeAdaptor
	{
		/**
		 * 
		 * @param node parent node
		 * @param mode inthis case only, left-right or right-left is relevant
		 * @return iterator over children collection, null or empty iterator if none
		 */
		public Iterator children(Object node);
	}
	
	public boolean hasNext()
	{
		return currentNode != null;
	}
	
	public Object next()
	{
		Object result = currentNode;
		findNextNode();
		
		return result;
	}
	
	private void findNextNode()
	{
		if (children.hasNext())
		{
			Object nextChild = children.next();
			
			Iterator grandChildren = adaptor.children(nextChild);
			
			if (isEmpty(grandChildren)) //nextChild is a leaf
			{
				currentNode = nextChild;
				return;
			}
			
			
			if ((mode & RIGHT_TO_LEFT) != 0)
			{
				grandChildren = reverse(grandChildren);
			}
			
			path.push(new NodeInfo(nextChild, children));
			children = grandChildren;

			if ((mode & BOTTOM_TOP) != 0)//children first
			{
				findNextNode();
			}
			else 
				currentNode = nextChild;
			return;	 
		}
		
		// if children don't have next
		
		if (path.size() == 0)
		{
		  currentNode = null;
		  return;
		}
		
		NodeInfo info = (NodeInfo)path.pop();
		children = info.children;

		if ((mode & BOTTOM_TOP) != 0)//children first
		{
			currentNode = info.node;
			return;
		}
		
		findNextNode();  
		
	}
	
	static final class NodeInfo
	{
		NodeInfo(Object node, Iterator children)
		{
			this.node = node;
			this.children = children;
		}
		Object node; 
		Iterator children;
	}	
	
	
	
	


}
