/*
 * Created on Nov 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * @author snshor
 *
 */
public final class TopoSort
{

	/**
	 * This method takes 2 equal size lists symbolizing 
	 * dependency graph, and topologically sorts it 
	 * 
	 * @param roots list of 
	 * @param leaves
	 * @return
	 */
	static  public List sort(List roots, List leaves) throws TopoSortCycleException
	{
		int len = roots.size();
		TopoSort ts = new TopoSort();
		for (int i = 0; i < len; i++)
		{
			ts.addOrderedPair(roots.get(i), leaves.get(i));
		}
		
		return ts.sort();
	}

	/**
	 * This method takes 2 equal size arrays symbolizing 
	 * dependency graph, and topologically sorts it 
	 * 
	 * @param roots list of 
	 * @param leaves
	 * @return
	 */
	static  public List sort(Object aryRoots, Object aryLeaves)throws TopoSortCycleException
	{
		int len = Array.getLength(aryRoots);
		TopoSort ts = new TopoSort();
		for (int i = 0; i < len; i++)
		{
			ts.addOrderedPair(Array.get(aryRoots,i), Array.get(aryLeaves,i));
		}
		
		return ts.sort();
	}
	
	/**
	 * This method takes Nx2-dimensional matrix symbolizing 
	 * dependency graph, and topologically sorts it 
	 * 
	 * @param roots list of 
	 * @param leaves
	 * @return
	 */
	static  public List sort(Object aryMatrix)throws TopoSortCycleException
	{
		int len = Array.getLength(aryMatrix);
		TopoSort ts = new TopoSort();
		for (int i = 0; i < len; i++)
		{
			Object pair = Array.get(aryMatrix, i); 
			ts.addOrderedPair(Array.get(pair,0), Array.get(pair,1));
		}
		
		return ts.sort();
	}
	

	/**
	 * This method takes Nx2-dimensional matrix symbolizing 
	 * dependency graph, and topologically sorts it 
	 * 
	 * @param roots list of 
	 * @param leaves
	 * @return
	 */
	static  public List sort(IPair[] pairs)throws TopoSortCycleException
	{
		int len = pairs.length;
		TopoSort ts = new TopoSort();
		for (int i = 0; i < len; i++)
		{
			ts.addOrderedPair(pairs[i].getRoot(), pairs[i].getLeaf());
		}
		
		return ts.sort();
	}
	

	

	ArrayList roots = new ArrayList();
	HashMap leaves = new HashMap();
	HashMap dependents = new HashMap();


	/**
	 * 
	 * @param root root object
	 * @param leaf object or null
	 */
	public void addOrderedPair(Object root, Object leaf)
	{

		//add dependent

		if (leaf != null)
		{
			Counter cnt = (Counter) leaves.get(leaf);
			if (cnt == null)
			{
				cnt = new Counter();
				leaves.put(leaf, cnt);
			}

			++cnt.cnt;
			roots.remove(leaf);
			// add dependency

			List deps = (List) dependents.get(root);
			if (deps == null)
			{
				deps = new ArrayList();
				dependents.put(root, deps);
			}
			deps.add(leaf);

		}


		if (!roots.contains(root) && !leaves.containsKey(root))
			roots.add(root);
	}

	public ArrayList sort() throws TopoSortCycleException
	{
		ArrayList res = new ArrayList();
		while (true)
		{
			int roots_size = roots.size(); 
			if (roots_size == 0)
			{
				if (leaves.size() > 0)
				{
					throw new TopoSortCycleException(leaves.keySet());
				}

				return res;
			}

			
			Object root = roots.get(roots_size - 1);

			res.add(root);
			roots.remove(roots_size - 1);

			List deps = (List) dependents.get(root);

			if (deps != null)
				for (Iterator iter = deps.iterator(); iter.hasNext();)
				{
					Object dep = iter.next();
					Counter cnt = (Counter) leaves.get(dep);

					if (--cnt.cnt == 0)
					{
						leaves.remove(dep);
						roots.add(dep);
					}

				}

		}

	}

	static class Counter
	{
		int cnt;
	}

	public interface IPair
	{
		Object getRoot();
		Object getLeaf();
	}

}
