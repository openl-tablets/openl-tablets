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
public interface ISelector
{
	public boolean select(Object obj);
	public ISelector not();
	public ISelector or(ISelector isel);
	public ISelector and(ISelector isel);
	public ISelector xor(ISelector isel);
}
