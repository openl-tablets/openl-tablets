/*
 * Created on Jan 13, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class SubTextSourceCodeModule implements IOpenSourceCodeModule
{

	IOpenSourceCodeModule baseModule; 
	int startPosition;


	


	/**
	 * 
	 */
	public SubTextSourceCodeModule(IOpenSourceCodeModule baseModule, int startPosition)
	{
		this.baseModule = baseModule;
		this.startPosition = startPosition;
	}
	
	
	

	/**
	 *
	 */

	public InputStream getByteStream()
	{
		return null;
	}


	boolean skipStart = false;	

	public Reader getCharacterStream()
	{
		Reader r = baseModule.getCharacterStream();
		if (!skipStart)
		{
			
			for (int i = 0; i < startPosition; i++)
			{
				try
				{
					r.read();
				}
				catch (IOException e)
				{
				}
			}
			skipStart = true;
		}
		return r;
	}

	/**
	 *
	 */

	public String getCode()
	{
		return baseModule.getCode().substring(startPosition);
	}

	/**
	 *
	 */

	public int getStartPosition()
	{
		return startPosition;
	}

	/**
	 *
	 */

	public int getTabSize()
	{
		return baseModule.getTabSize();
	}

	/**
	 *
	 */

	public String getUri(int textpos)
	{
		return baseModule.getUri(textpos + startPosition);
	}

}
