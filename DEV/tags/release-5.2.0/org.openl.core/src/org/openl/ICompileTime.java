/*
 * Created on Aug 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl;

/**
 * @author snshor
 *
 */
public interface ICompileTime
{
	IOpenParser getParser();
	
	IOpenBinder getBinder();


	void extend(ICompileTime ict);

}
