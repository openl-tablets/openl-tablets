/*
 * Created on Jul 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.io.File;
import java.util.Iterator;


/**
 * @author snshor
 *
 */
public class FileTreeIterator extends TreeIterator
{


  /**
   * @param treeRoot
   * @param adaptor
   * @param mode
   */
  public FileTreeIterator(File root, int mode)
  {
    super(root, new FileTreeAdaptor(), mode);
  }
  
  
  static class FileTreeAdaptor implements TreeIterator.TreeAdaptor
  {
  	
      /* (non-Javadoc)
     * @see org.openl.util.TreeIterator.TreeAdaptor#children(java.lang.Object)
     */
    public Iterator children(Object node)
    {
    	File f = (File) node;
    	if (!f.isDirectory())
    	  return null;
      return   OpenIterator.fromArray(f.listFiles());
    }

}


	public File nextFile()
	{
		return (File)next();
	}
 
}
