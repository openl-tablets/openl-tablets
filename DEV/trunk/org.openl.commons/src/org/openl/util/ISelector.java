/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

/**
 * @author snshor
 *
 */
public interface ISelector<T>
{
	public boolean select(T obj);
	public ISelector<T> not();
	public ISelector<T> or(ISelector<T> isel);
	public ISelector<T> and(ISelector<T> isel);
	public ISelector<T> xor(ISelector<T> isel);
}
