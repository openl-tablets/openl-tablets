/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

import java.util.HashMap;
import java.util.Stack;

/**
 * @author snshor
 *
 */
public class CategorizedSearchContext implements ICategorizedSearchContext
{

	static ThreadLocal contexts = new ThreadLocal();	
	
	
	public static ICategorizedSearchContext current()
	{
		 Stack  s = (Stack)contexts.get();
		 if (s == null || s.size() == 0)
			 return defaultSearchContext();
		 return (ICategorizedSearchContext)s.peek();
	}

	
	public static void push(ICategorizedSearchContext cxt)
	{
		 Stack  s = (Stack)contexts.get();
		 if (s == null)
		 {
			 s = new Stack();
			 contexts.set(s);
		 }
		 s.push(cxt);
		
	}

	public static void pushThis()
	{
		 push(new CategorizedSearchContext(current()));
	}
	
	
	/**
	 * @return
	 */
	private static synchronized ICategorizedSearchContext defaultSearchContext()
	{
		if (defaultContext == null)
		{
			defaultContext = new CategorizedSearchContext(null);
			initDefaultContext();
		}	
		return defaultContext;
	}
	
	
	

	/**
	 * 
	 */
	private static void initDefaultContext()
	{
		// TODO Auto-generated method stub
		
	}




	static CategorizedSearchContext defaultContext;;
	

	public CategorizedSearchContext(ICategorizedSearchContext context)
	{
		this.parent = context;
	}


	ICategorizedSearchContext parent;
	
	
	public ICategorizedSearchContext getParent()
	{
		return parent;
	}

	HashMap map = new HashMap();
	
	static class CKey 
	{
		Object key;
		String category;
		
		CKey(Object key, String category)
		{
			this.key = key;
			this.category = category;
		}

		public boolean equals(Object obj)
		{
			CKey ckey =  (CKey)obj;
			return key.equals(ckey.key) && category.equals(ckey.category);
		}

		public int hashCode()
		{
			return key.hashCode() + 37 * category.hashCode();
		}
	}
	
	public void register(Object key, String category, Object value)
	{
		map.put(new CKey(key,category), value);
	}
	
	public void unregister(Object key, String category)
	{
		map.remove(new CKey(key,category));
	}
	
	
	public Object findLocal(Object key, String category)
	{
		CKey ckey = new CKey(key,category);
		return map.get(ckey);
	}


	/* 
	 * primitive implemention, later we may improve it with key/category iteration to look for things like super classes/super interfaces
	 */
	public Object find(Object key, String category)
	{
		Object res = findLocal(key, category);
		if (res != null)
			return res;
		if (parent != null)
			return parent.find(key, category);
		return null;
	}

	
	
	
}
