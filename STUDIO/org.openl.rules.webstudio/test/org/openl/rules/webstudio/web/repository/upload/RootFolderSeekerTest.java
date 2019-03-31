package org.openl.rules.webstudio.web.repository.upload;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class RootFolderSeekerTest {

    @Test
    public void test() {
        Set<String> folderNames = new HashSet<String>();
        folderNames.add("my/");
        folderNames.add("my/file.txt");
        folderNames.add("my/very_very_very_big_file_name.xml");
        folderNames.add("my/package/");
        folderNames.add("my/package/");
        folderNames.add("my/package/");
        folderNames.add("my/package/newFile.owl");
        folderNames.add("my/package/rules/");
        folderNames.add("my/package/rules/rule.xls");
        RootFolderExtractor folderExtractor = new RootFolderExtractor(folderNames, null);

        assertEquals("package/hello/", folderExtractor.extractFromRootFolder("my/package/hello/"));
        assertEquals("file.xml", folderExtractor.extractFromRootFolder("my/file.xml"));
        assertEquals(null, folderExtractor.extractFromRootFolder("file.txt"));
        assertEquals(null, folderExtractor.extractFromRootFolder("hello/package/hello/"));
        assertEquals(null, folderExtractor.extractFromRootFolder(null));
    }

    @Test
    public void testFilesInRootDirectory() {
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
        RootFolderExtractor folderExtractor = new RootFolderExtractor(folderNames, null);

        assertEquals("my/package/hello/", folderExtractor.extractFromRootFolder("my/package/hello/"));
        assertEquals("my/file.xml", folderExtractor.extractFromRootFolder("my/file.xml"));
        assertEquals("file.txt", folderExtractor.extractFromRootFolder("file.txt"));
        assertEquals("hello/package/hello/", folderExtractor.extractFromRootFolder("hello/package/hello/"));
        assertEquals(null, folderExtractor.extractFromRootFolder(null));
    }

}
