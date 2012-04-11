package org.openl.rules.webstudio.web.repository.upload;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;

public class RootFolderSeekerTest {
    
    private static RootFolderExtractor folderExtractor = null;
    
    @Before
    public void initFolderExtractor() {
        Set<String> folderNames = initFolderNames();
        
        folderExtractor = new RootFolderExtractor(folderNames);
    }
    
    @Test
    public void test() {
        assertEquals("package/hello/", folderExtractor.extractFromRootFolder("my/package/hello/"));
        assertEquals("file.xml", folderExtractor.extractFromRootFolder("my/file.xml"));
        assertEquals(null, folderExtractor.extractFromRootFolder("hello/package/hello/"));
        assertEquals(null, folderExtractor.extractFromRootFolder(null));
    }

    private Set<String> initFolderNames() {
        Set<String> folderNames = new HashSet<String>();
        folderNames.add("my/");
        folderNames.add("file.txt");
        folderNames.add("very_very_very_big_file_name.xml");
        folderNames.add("my/package/");
        folderNames.add("my/package/");
        folderNames.add("my/package/");
        folderNames.add("my/package/newFile.owl");
        folderNames.add("my/package/rules/");
        folderNames.add("my/package/rules/rule.xls");
        return folderNames;
    }

}
