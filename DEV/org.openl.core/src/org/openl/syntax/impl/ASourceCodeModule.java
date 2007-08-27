/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.syntax.impl;

import java.io.BufferedReader;
import java.io.IOException;

import org.openl.IOpenSourceCodeModule;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 */
public abstract class ASourceCodeModule implements
    IOpenSourceCodeModule
{
  
  public String getCode()
  {
  	
  	
  	if (code == null)
  	{
    	StringBuffer buf = new StringBuffer(4096);
      	char[] c = new char[4096];
  		BufferedReader br = new BufferedReader(  getCharacterStream());
			
      try
      {
        for(int len;   (len =  br.read(c)) > 0; )
          buf.append(c, 0, len);
      }
      catch (IOException e)
      {
        throw RuntimeExceptionWrapper.wrap(e);
      }
  		code = buf.toString();   
  	}
  	return code;
  }
  
  String code;
  
  int tabSize = 2;
  
  

  /**
   * @return Returns the tabSize.
   */
  public int getTabSize()
  {
    return tabSize;
  }
  /**
   * @param tabSize The tabSize to set.
   */
  public void setTabSize(int tabSize)
  {
    this.tabSize = tabSize;
  }
  
	public int getStartPosition()
	{
		return 0;
	}

}
