/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.openl.rules.ui.search.FileIndexer;
import org.openl.rules.webstudio.util.WebstudioTreeIterator;
import org.openl.util.FileTypeHelper;
import org.openl.util.Log;
import org.openl.util.tree.TreeIterator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Gets all the files from project directory and set it to {@link FileIndex} for further indexing .
 * 
 * @author snshor
 *
 */
public class ProjectIndexer extends FileIndexer {
    private String projectRoot;

    public ProjectIndexer(String projectRoot) {
        this.projectRoot = projectRoot;

        loadFiles();
    }
    
    /**
     * Find the max length between 'bin' and 'classes' folders in the given path. 
     * 
     * @param path Path to file.
     * @return
     */
    int findMaxPathLength(String path) {
        int idx1 = path.lastIndexOf("bin");
        int idx2 = path.lastIndexOf("classes");
        return Math.max(idx1, idx2);
    }
    
    /**
     * Iterates over project folder, searches for excel and word files.
     * 
     */
    synchronized void  loadFiles() {
		TreeIterator<File> filesTreeIter = new WebstudioTreeIterator(new File(projectRoot), 0);
		
		HashMap<String, String> fileMap = new HashMap<String, String>();
	    for (; filesTreeIter.hasNext();) {
			File file = filesTreeIter.next();
			String fileName = file.getName();
			if (!file.isHidden()
			        && (FileTypeHelper.isExcelFile(fileName) || FileTypeHelper.isWordFile(fileName)))
				try {
					String canonPath = file.getCanonicalPath(); 										
					String fileAlreadyExist = fileMap.get(fileName);					
					String preferrablePath = selectPreferrablePath(fileAlreadyExist, canonPath);					
					fileMap.put(fileName, preferrablePath);
			} catch (IOException e) {
					e.printStackTrace();
				}			
		}
	    
	    String[] files = new String[fileMap.size()];
	    
	    Iterator<String> it = fileMap.values().iterator();
	    for (int i = 0; i < files.length; i++) {
			files[i] = it.next();
		}	    
	    setFiles(files);		
	}

    @Override
    public void reset() {
        loadFiles();
    }

    /**
     * Preferable are files with min length to 'bin' or 'classes' folders in their path.
     * 
     * @param existingFile File that has been already found and put to map.
     * @param currentFile Path to current file.
     * @return
     */
    private String selectPreferrablePath(String existingFile, String currentFile) {
        if (existingFile == null) {
            return currentFile;
        }
        int existMaxLength = findMaxPathLength(existingFile);
        int currentMaxLength = findMaxPathLength(currentFile);
        if (existMaxLength == currentMaxLength) {
            Log.warn("Two files with the same name: \n" + existingFile + "\n" + currentFile);
        }

        return existMaxLength < currentMaxLength ? existingFile : currentFile;
    }

}
