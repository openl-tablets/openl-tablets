package org.openl.util;

import java.util.Iterator;
import java.util.TreeMap;

public class TreeElement implements ITreeElement.Node
{
	TreeMap elements = new TreeMap();
	
	public TreeElement(String type)
	{
		this.type = type;
	}
	
	String type;
	
	Object object;
	
	public Object getObject()
	{
		return object;
	}

	public void setObject(Object object)
	{
		this.object = object;
	}


	public Iterator getChildren()
	{
		return elements.values().iterator();
	}

	public void setElements(TreeMap children)
	{
		this.elements = children;
	}


	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public TreeMap getElements()
	{
		return elements;
	}

	public boolean isLeaf()
	{
		return elements == null || elements.size() == 0;
	}

	public ITreeElement getChild(Object key)
	{
		return (ITreeElement)elements.get(key);
	}

	
	
	public boolean addChild(Object key, ITreeElement child)
	{
		return elements.put(key, child) != null;
	}
}
