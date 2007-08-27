/*
 * Created on May 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

/**
 * @author snshor
 */

public abstract class AOpenIterator implements IOpenIterator
{

	static boolean isEmpty(Iterator it)
	{
		return it == null || it == EMPTY;
	}

	public static Iterator merge(Iterator it1, Iterator it2)
	{
		if (isEmpty(it1))
		{
			return it2 == null ? EMPTY : it2;
		}

		if (isEmpty(it2))
			return it1;

		return new MergeIterator(it1, it2);
	}

	public static IOpenIterator merge(IOpenIterator it1, IOpenIterator it2)
	{
		if (isEmpty(it1))
		{
			return it2 == null ? EMPTY : it2;
		}

		if (isEmpty(it2))
			return it1;

		return new MergeIterator(it1, it2);
	}
  
	public static List asList(Iterator it)
	{
		int size = size(it);
		List result = null;
		
		switch(size)
		{
			case 0:
				return Collections.EMPTY_LIST;
			case UNKNOWN_SIZE:
				result = new Vector();
				break;
			default:
				result = new Vector(size);   
		}
		
		for (; it.hasNext();)
		{
			result.add( it.next());
		}
		return result;  		
	}


	public static void evaluate(IBlock block, Iterator it)
	{
		while(it.hasNext())
		  block.evaluate(it.next());
	}

	public static Set asSet(Iterator it)
	{
		int size = size(it);
		Set result = null;
		
		switch(size)
		{
			case 0:
				return Collections.EMPTY_SET;
			case UNKNOWN_SIZE:
				result = new HashSet();
				break;
			default:
				result = new HashSet(size);   
		}
		
		for (; it.hasNext();)
		{
			result.add( it.next());
		}
		return result;  		
	}



  public static IOpenIterator asOpenIterator(Iterator it)
  {
    if (it == null)
      return EMPTY;

    if (it instanceof IOpenIterator)
    {
      return (IOpenIterator)it;
    }

    return new IteratorWrapper(it);
  }

  public static int size(Iterator it)
  {
    if (it instanceof IOpenIterator)
      return ((IOpenIterator)it).size();
    return UNKNOWN_SIZE;
  }

  static public IOpenIterator reverse(Iterator it)
  {
    if (it instanceof IOpenIterator)
      return ((IOpenIterator)it).reverse();
    throw new UnsupportedOperationException();
  }


	static public IOpenIterator extend(Iterator it, IOpenIteratorExtender mod)
	{
		return new ModifierIterator(it, mod);
	}

	

  static public IOpenIterator select(Iterator it, ISelector is)
  {
    return new SelectIterator(it, is);
  }

  static public IOpenIterator collect(Iterator it, IConvertor ic)
  {
    return new CollectIterator(it, ic);
  }



  static public int store(Iterator it, IAppender appender)
  {
    int cnt = 0;
    for (; it.hasNext() && appender.add(it.next()); ++cnt);
    return cnt;
  }
  
  

	public void evaluate(IBlock block)
	{
		evaluate(block, this);
	}	

  public int store(IAppender appender)
  {
    return store(this, appender);
  }
  
  public Set asSet()
  {
  	return asSet(this);	
  }

	public List asList()
	{
		return asList(this);	
	}

  public static Iterator sort(Iterator it, Comparator cmp)
  {
    int size = size(it);
    switch (size)
    {
      case UNKNOWN_SIZE :
        {
          Vector v = new Vector();
          store(it, Appender.toCollection(v));
          Collections.sort(v, cmp);
          return v.iterator();
        }
      case 0 :
        return EMPTY;
      case 1 :
        return it;
      default :
        {
          Object[] ary = new Object[size];
          store(it, Appender.toArray(ary));
          Arrays.sort(ary, cmp);
          return new AIndexedIterator.ArrayIterator(ary);
        }

    }
  }
  
  public IOpenIterator sort(Comparator cmp)
  {
  	return asOpenIterator(sort(this, cmp));
  }

  public void remove()
  {
    throw new IllegalStateException();
  }

  public Iterator append(Iterator it)
  {
    return it == null || it == EMPTY ? this : merge(this, it);
  }

	public IOpenIterator append(IOpenIterator it)
	{
		return it == null || it == EMPTY ? this : merge(this, it);
	}

  /**
   * Returns reverse iterator ri such as last(this) == first(ri), last-1(this) == first+1(ri), 
   * this.size() = ri.size(), this.count() = ri.count() etc.
   * @return
   */

  public IOpenIterator reverse()
  {
    throw new UnsupportedOperationException();
  }

  public IOpenIterator select(ISelector is)
  {
    return new SelectIterator(this, is);
  }

  public IOpenIterator collect(IConvertor ic)
  {
    return new CollectIterator(this, ic);
  }

  public IOpenIterator convert(IConvertor ic)
  {
		return collect(ic);
  }


  /**
   * Calculates the number of iterated elements. 
   * Unfortunately, destroys the iterator
   * @see #size 
   * @return
   */

  public int count()
  {
    int cnt = 0;
    for (; this.hasNext(); ++cnt)
    {
      this.next();
    }
    return cnt;
  }

  /**
   * Calculates the remaining size of iterated collection without destroying itself(const in c++ terminology), 
   * -1 if it can not be known in advance. Not every iterator is capable of doing it.
   * 
   * @see count
   */

  public int size()
  {
    return UNKNOWN_SIZE;
  }

  /**
   * Skips up to n elements. 
   * @param n
   * @return actual number of skipped elements
   */
  public int skip(int n)
  {
    int x = n;
    for (; n > 0 && this.hasNext(); n--)
    {
      this.next();
    }

    return x - n;

  }

  //////////////////////////////// Some useful OpenIterators ///////////////////////////////////////////

  static final public EmptyIterator EMPTY = new EmptyIterator();

  static final class EmptyIterator extends AOpenIterator
  {
    public boolean hasNext()
    {
      return false;
    }

    public IOpenIterator select(ISelector is)
    {
      return this;
    }

    public IOpenIterator collect(IConvertor ic)
    {
      return this;
    }

    public Object next()
    {
      throw new NoSuchElementException("EmptyIterator");
    }

    public int size()
    {
      return 0;
    }

    public IOpenIterator reverse()
    {
      return this;
    }

  }

  /**
   * 
   * @param value
   * @return iterator over single value, if value != null, empty iterator otherwise
   */
  public static AOpenIterator single(Object value)
  {
    if (value == null)
      return EMPTY;
    return new SingleIterator(value);
  }

  /**
   * Iterates over single object exactly one time.
   */
  static final class SingleIterator extends AOpenIterator
  {
    Object value;

    SingleIterator(Object value)
    {
      this.value = value;
    }

    public boolean hasNext()
    {
      return value != null;
    }

    public Object next()
    {
      if (value == null)
        throw new NoSuchElementException();
      Object tmp = value;
      value = null;
      return tmp;
    }

    public final int size()
    {
      return value == null ? 0 : 1;
    }

    public IOpenIterator reverse()
    {
      if (size() == 0)
        return EMPTY;
      return this;
    }

  }

  static public class MergeIterator extends AOpenIterator
  {
    Iterator[] itt;
    int current = 0;

    public MergeIterator(Iterator it1, Iterator it2)
    {
      this.itt = new Iterator[]{it1, it2};
    }

    public MergeIterator(Iterator[] itt)
    {
      this.itt = itt;
    }

    
    public boolean hasNext()
    {
    	for(  ; current < itt.length; ++current)
    	{
    		if (itt[current].hasNext())
    			return true;
    	}	
   
    	return false;
    }

    public Object next()
    {
      return itt[current].next();
    }

    public void remove()
    {
      itt[current].remove();
    }

    public int size()
    {

    	int total = 0;
    	for (int i = current; i < itt.length; i++)
			{
				int size = size(itt[i]);
				if (size == UNKNOWN_SIZE)
					return UNKNOWN_SIZE;
				total += size;
			}
    	
    	return total;
    	
    }
  }

  static final class SelectIterator extends IteratorWrapper
  {
    ISelector selector;
    Object next;
    boolean hasNext = false;

    SelectIterator(Iterator it, ISelector selector)
    {
      super(it);
      this.selector = selector;
    }

    void findNext()
    {
      while (it.hasNext())
      {
        Object obj = it.next();
        if (selector.select(obj))
        {
          next = obj;
          hasNext = true;
          return;
        }
      }

      next = null;
      hasNext = false;
    }

    public boolean hasNext()
	  {
	  	if (!hasNext)
	  	  findNext();
      return hasNext;
    }

    public Object next()
    {
    	if (!hasNext())
    	  throw new IllegalStateException();
    	hasNext = false;  
      return next;
    }
    
    
  }

  
  
  
  
  static final class CollectIterator extends IteratorWrapper
  {
    IConvertor collector;

    CollectIterator(Iterator it, IConvertor selector)
    {
      super(it);
      this.collector = selector;
    }

    public Object next()
    {
      return collector.convert(it.next());
    }
  }
  
  
  static class IteratorWrapper extends AOpenIterator
	{
		IteratorWrapper(Iterator it)
		{
			this.it = it;
		}
		
		protected Iterator it;


		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext()
		{
			return it.hasNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Object next()
		{
			return it.next();
		}
	}
	static class ModifierIterator extends AOpenIterator
	{
		Iterator baseIterator;
		IOpenIteratorExtender modifier;
		boolean hasNext = false;
		Object next = null;
		Iterator modifierIterator;
		
		
		ModifierIterator(Iterator baseIterator, IOpenIteratorExtender modifier)
		{
			this.baseIterator = baseIterator;
			this.modifier = modifier;
		}
		
		
		protected void findNextObject()
		{
			while(modifierIterator == null || !modifierIterator.hasNext())
			{
				if (modifierIterator == null)
				{
					modifierIterator = getNextIterator();
					if (modifierIterator == null)
						return;
				}
			
				if (!modifierIterator.hasNext())
					modifierIterator = null;
			}
			
			next = modifierIterator.next();  
			hasNext = true;  
		}
		
	 protected Iterator getNextIterator()
	 {
		 while(baseIterator.hasNext())
		 {
			 Iterator it = modifier.extend(baseIterator.next());
			 if (it != null)
				 return it;
		 }
		 return null;
	 }	
		
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext()
		{
			if (hasNext)
				return true;
			findNextObject();  
			return hasNext;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Object next()
		{
			if (!hasNext())
				throw new IllegalStateException();
			hasNext = false; 	
			return next;
		}

	}
	
	
  /* (non-Javadoc)
   * @see org.openl.util.IOpenIterator#modify(org.openl.util.IOpenIteratorModifier)
   */
  public IOpenIterator extend(IOpenIteratorExtender mod)
  {
    return extend(this, mod);
  }

}
