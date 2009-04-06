/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

/**
 * @author snshor
 *
 */
public class JavaLang extends JavaImportTypeLibrary
{
	public JavaLang()
	{
		super(null, new String[]{"java.lang"}, ClassLoader.getSystemClassLoader());
	}

}
