package org.openl.rules.webstudio.web.repository.upload;

import java.util.Set;

/**
 * Ectractor for folder name paths from the top root folder.
 * 
 * @author DLiauchuk
 *
 */
public class RootFolderExtractor {
    
    private Set<String> folderNames;
    private String rootName;
    
    public RootFolderExtractor(Set<String> folderNames) {        
        this.folderNames = folderNames;
        initRootFolderPath();
    }
    
    /**
     * Inits the root folder name is exists from the set of folder names. 
     */
    private void initRootFolderPath() {
        if (needToExtract()) {
            for (String folderName : folderNames) {
                int ind = folderName.indexOf('/');
                if (ind > 0) {
                    rootName = folderName.substring(0, ind + 1);
                    return;
                }
            }
        }
    }
    
    /**
     * Extracs the folder from the root folder(if exists).
     
     * 
     * @param folderName folder name to be extracted from the root folder.
     * @return extracted folder name from the root folder.<br> 
     * E.g. folderName: <code>org/package/sources/</code>.
     * if the root folder is <code>org</code>, the result will be <code>package/sources/</code>.
     */
    public String extractFromRootFolder(String folderName) {
        String result = null;
        if (rootName != null) {
            if (folderName != null && folderName.startsWith(rootName) && !folderName.equals(rootName)) {
                result = folderName.substring(rootName.length());
            }
            return result;
        }
        return folderName;
    }
    
    /**
     * Check if there is a single root folder in the set of folder names.
     * Algorithm: if there is only one folder path which contains first found symbol '/', and it`s number
     * is equal to path length.
     * 
     * @return true if there is a single root folder in the set of folder names
     */
    private boolean needToExtract() {
        int cnt = 0;        
        for (String name : folderNames) {
            if (name.indexOf('/') == (name.length() - 1)) {
                cnt++;
            }
        }
        if (cnt > 1) {
            return false;
        } else {
            return true;
        }
    }


}
