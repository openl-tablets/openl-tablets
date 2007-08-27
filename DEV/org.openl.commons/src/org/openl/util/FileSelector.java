/**
 * Created Oct 22, 2006
 */
package org.openl.util;

import java.io.File;

/**
 * Companion to FileTreeIterator
 * 
 * @author snshor
 *
 */
public abstract class FileSelector extends ASelector
{

	public boolean select(Object obj)
	{
		return selectFile((File)obj);
	}

	/**
	 * @param file
	 * @return
	 */
	public abstract boolean selectFile(File file);


}
