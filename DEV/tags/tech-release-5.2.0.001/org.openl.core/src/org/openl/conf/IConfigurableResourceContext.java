/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import java.io.File;
import java.net.URL;

/**
 * @author snshor
 *
 */
public interface IConfigurableResourceContext
{
	public String findProperty(String propertyName);
	public URL findClassPathResource(String url);
	public Class findClass(String className);
	public File findFileSystemResource(String url);
	
	public ClassLoader getClassLoader();
	
	
	IOpenLConfiguration getConfiguration();
	
}
