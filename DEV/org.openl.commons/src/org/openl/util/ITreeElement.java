package org.openl.util;

import java.util.Iterator;

public interface ITreeElement
{
	String getType();
//	String getDisplayValue();
	Object getObject();
//	TreeMap getElementsx();
	Iterator getChildren();
	boolean isLeaf();
	
	static  public interface Node extends ITreeElement
	{
		ITreeElement getChild(Object key);
		boolean addChild(Object key, ITreeElement child);
	}
}
