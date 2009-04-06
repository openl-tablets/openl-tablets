package org.openl.util;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.openl.util.meta.IOrderMetaInfo;

/*
 * Created on May 12, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

/**
 * @author snshor
 */
public class SkipList implements Map
{
	/**
	 * 
	 */
	public SkipList()
	{
		this(IOrderMetaInfo.DEFAULT_COMPARATOR, 0.3, 10);
		
	}

	Comparator keyComparator;
	double nodeRatio;
	int maxIndexLevel;

	int indexLevel = 0;

	ISkipListNode header;

	int size = 0;

	public SkipList(Comparator keyComparator, double nodeRatio, int maxLevel)
	{
		this.keyComparator = keyComparator;
		this.nodeRatio = nodeRatio;
		this.maxIndexLevel = maxLevel;
		clear();
	}

	/**
	 *
	 */

	public void clear()
	{
		header = makeSkipListNode(null, null, maxIndexLevel+1);
		size = 0;
		indexLevel = 0;
	}

	/**
	 *
	 */

	public boolean containsKey(Object key)
	{
		ISkipListNode inode = findNodeGE(key);
		return hasKey(inode, key);
	}

	final int compare(Object myKey, Object key)
	{
		return myKey == null ? -1 : keyComparator.compare(myKey, key);
	}

	ISkipListNode findNodeGE(Object searchKey)
	{
		ISkipListNode x = header;
		// loop invariant: x.key < searchKey
		for (int i = indexLevel; i >= 0; --i)
			while (x.next(i) != null
				&& compare(x.next(i).getKey(), searchKey) < 0)
				x = x.next(i);

		x = x.next(0);

		return x;
	}

	/**
	 *
	 */

	public boolean containsValue(Object value)
	{

		for (ISkipListNode node = header.next(0);
			node != null;
			node = node.next(0))
		{
			if (value.equals(node.getValue()))
				return true;
		}
		return false;
	}

	/**
	 *
	 */

	public Set entrySet()
	{
		throw new UnsupportedOperationException();
	}

	final boolean hasKey(ISkipListNode inode, Object key)
	{
		return inode == null
			|| inode == header ? false : inode.getKey().equals(key);
	}

	/**
	 *
	 */

	public Object get(Object key)
	{
		ISkipListNode inode = findNodeGE(key);

		if (hasKey(inode, key))
			return inode.getValue();
		return null;
	}

	/**
	 *
	 */

	public boolean isEmpty()
	{
		return size == 0;
	}

	/**
	 *
	 */

	public Set keySet()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 */

	public Object put(Object searchKey, Object newValue)
	{
		ISkipListNode[] path = new ISkipListNode[maxIndexLevel + 1];
		ISkipListNode x = header;
		for (int i = indexLevel; i >= 0; --i)
		{
			while (x.next(i) != null
				&& compare(x.next(i).getKey(), searchKey) < 0)
				x = x.next(i);
			path[i] = x;
		}
		//-- x®key < searchKey £x®forward[i]®key
		x = x.next(0);
		if (hasKey(x, searchKey))
		{
			Object oldValue = x.getValue();
			x.setValue(newValue); //x®key = searchKey then x®value := newValue
			return oldValue;
		}
		else
		{
			++size;
			int lvl = randomLevel();
			if (lvl > indexLevel)
			{
				for (int i = indexLevel + 1; i <= lvl; ++i)
					path[i] = header;
				indexLevel = lvl;
			}
			x = makeSkipListNode(searchKey, newValue, lvl);
			for (int i = 0; i <= lvl; ++i)
			{
				x.setNext(path[i].next(i), i);
				path[i].setNext(x, i);
			}
		}
		return null;
	}
	/**
	 *
	 */

	Random random = new Random(0);

	/**
	 * @return
	 */
	private int randomLevel()
	{
		
		int max = Math.min(maxIndexLevel, indexLevel + 1);
		int i = 0;
		for(; i < max; ++i)
		{
			double d = random.nextDouble();
			if (d > nodeRatio)
			  return i;
		}
			
		return i;
	}

	public void putAll(Map t)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 */

	public Object remove(Object key)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 */

	public int size()
	{
		return size;
	}

	/**
	 *
	 */

	public Collection values()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	static public interface ISkipListNode extends Map.Entry
	{
		ISkipListNode next(int level);
		void setNext(ISkipListNode node, int level);
		//		SkipListNode previous(int level);
		int getLevel();

	}

	protected ISkipListNode makeSkipListNode(
		Object key,
		Object value,
		int level)
	{
		return new SkipListNode(key, value, level + 1);
	}

	static class SkipListNode implements ISkipListNode
	{

		ISkipListNode[] nodes;
		Object key, value;
		/**
			 *
			 */

		SkipListNode(Object key, Object value, int level)
		{
			this.key = key;
			this.value = value;
			nodes = new ISkipListNode[level];
		}

		public int getLevel()
		{
			return nodes.length;
		}

		/**
		 *
		 */

		public ISkipListNode next(int level)
		{
			return nodes[level];
		}

		/**
		 *
		 */

		public void setNext(ISkipListNode node, int level)
		{
			nodes[level] = node;
		}

		/**
		 *
		 */

		public Object getKey()
		{
			return key;
		}

		/**
		 *
		 */

		public Object getValue()
		{
			return value;
		}

		/**
		 *
		 */

		public Object setValue(Object value)
		{
			// TODO Auto-generated method stub
			return null;
		}

	}

}
