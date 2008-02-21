/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl;

import java.io.InputStream;
import java.io.Reader;

/**
 * @author snshor
 *
 */
public interface IOpenSourceCodeModule 
{

	String getCode();
	/**
	 * 
	 * @return relative start position within a module
	 */
	int getStartPosition();

	String getUri(int textpos);
	
	InputStream getByteStream();
	
	Reader getCharacterStream();
	
	int getTabSize();
	
}
