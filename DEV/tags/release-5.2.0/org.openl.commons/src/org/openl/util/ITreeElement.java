package org.openl.util;

import java.util.Iterator;

public interface ITreeElement<T>
{
	String getType();
//	String getDisplayValue();
	T getObject();
//	TreeMap getElementsx();
	Iterator<T> getChildren();
	boolean isLeaf();
	
	static  public interface Node<T> extends ITreeElement<T>
	{
		ITreeElement<T> getChild(Object key);
		boolean addChild(Object key, ITreeElement<T> child);
	}
}
