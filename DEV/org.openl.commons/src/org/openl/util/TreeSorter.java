package org.openl.util;

public abstract class TreeSorter
{

	public abstract Comparable makeKey(Object obj);

	public abstract ITreeElement makeElement(Object obj, int i);
	
	static public void addElement(ITreeElement targetElement, Object obj, TreeSorter[] sorters, int level)
	{
		if (level >= sorters.length)
			return;
		 
		ITreeElement.Node target = (ITreeElement.Node)targetElement;
		Comparable key = sorters[level].makeKey(obj);
		ITreeElement element = (ITreeElement) target.getChild(key);
		if (element == null )
		{
			element = sorters[level].makeElement(obj, 0);
			target.addChild(key, element);
		}
		else if (sorters[level].isUnique())
		{
			for(int i = 2; i < 100; ++i)
			{
				Comparable key2 = sorters[level].makeKey(obj, i);
				element = (ITreeElement) target.getChild(key2);
				if (element == null)
				{
					element = sorters[level].makeElement(obj, i);
					target.addChild(key2, element);
					break;
				}	
			}	
		}	
		
		addElement(element, obj, sorters, level + 1);
	}

	/**
	 * @param obj
	 * @param i
	 * @return
	 */
	protected Comparable makeKey(Object obj, int i)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @return
	 */
	protected boolean isUnique()
	{
		return false;
	}
	
	
}
