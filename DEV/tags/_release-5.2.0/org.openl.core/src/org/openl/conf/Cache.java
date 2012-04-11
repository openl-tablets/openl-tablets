/*
 * Created on Jun 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

/**
 * @author snshor
 *
 */
public class Cache
{

	static public Object makeKey(Object[] objs)
	{
		return new GenericKey(objs);
	}

	
	static public Object makeKey(Object obj1, Object obj2)
	{
		return new GenericKey(new Object[]{obj1, obj2});
	}

	static public Object makeKey(Object obj1, Object obj2, Object obj3)
	{
		return new GenericKey(new Object[]{obj1, obj2, obj3});
	}
	
	static final class GenericKey
	{
		Object[] obj;
		GenericKey(Object[] obj)
		{
			this.obj = obj;
		}
		
		public boolean equals(Object x)
		{
			if (x != null && x instanceof GenericKey)
			{
				return obj.length == ((GenericKey)x).obj.length  && compare(((GenericKey)x).obj);
			}
			return false;
		}
		
		boolean compare(Object[] anObj)
		{
			for (int i = 0; i < anObj.length; i++)
			{
				if (!obj[i].equals(anObj[i]))
					return false;
			}
			return true;
		}
		
		public int hashCode()
		{
			int x = 17;
			
			for (int i = 0; i < obj.length; i++)
			{
				x += obj[i].hashCode() + 37;
			} 
			return x;
		} 
	}
	


}
