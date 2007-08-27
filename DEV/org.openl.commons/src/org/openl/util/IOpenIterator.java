/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author snshor
 *
 */
public interface IOpenIterator extends Iterator
{
	public static final int UNKNOWN_SIZE = -1;
	
	public List asList();
	
	public Set asSet();
	
	
	public IOpenIterator select(ISelector sel); 
	public IOpenIterator extend(IOpenIteratorExtender mod); 
	
	/**
	 * Legacy (Smalltalk) name, same as convert
	 * @param col
	 * @return
	 */
	public IOpenIterator collect(IConvertor col);
	/**
	 * Same as collect
	 * @param col
	 * @return
	 */
	public IOpenIterator convert(IConvertor col);
	public void evaluate(IBlock block);
	
	/**
	 * @return the number of elements in iterator, it is not a "const" method, performs it by actual enumeration 
	 */
	
	public int count();
	
	/**
	 * @return the number of elements left to iterate, or UNKNOWN_SIZE if it is not known, this method is "const" 
	 */
	
	public int size();
	
	public IOpenIterator sort(Comparator cmp);
	
	public Iterator append(Iterator it);

	public IOpenIterator append(IOpenIterator it);
		
  public IOpenIterator reverse() throws UnsupportedOperationException;

	public int skip(int n);
	

}
