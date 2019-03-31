package org.openl.rules.workspace.filter;

import junit.framework.TestCase;

import org.openl.rules.workspace.filter.FolderNamePathFilter;
import org.openl.rules.workspace.filter.PathFilter;

import java.util.Arrays;

public class FolderUploadFilter_VCS_FILES_FILTER_TestCase extends TestCase {
    private PathFilter instance;

    @Override
    protected void setUp() throws Exception {
        instance = new AndPathFilter(Arrays.asList(new FolderNamePathFilter(Arrays.asList(".svn", "CVS")),
            new FileNamePathFilter(Arrays.asList(".cvsignore"))));
    }

    public void testAccepted() {
        final String[] params = { "f",
                "f/",
                "svn",
                "svn/",
                "svn/hello.java",
                ".xorg/xorg.conf",
                "CVS",
                "cvs/",
                ".cvs/",
                "cvs/hello.java",
                "a/b/c/d/e/f/g/h/i.txt",
                "root/file.cvs",
                "root/file.svn",
                "root/svn/1.txt",
                "root/.CVS/1.txt",
                ".svn./1",
                "root/folder/.svnt/",
                "CVS./1",
                "root/folder/.CVSNT/",
                ".svn",
                "root/.svn",
                "cVS/",
                "root/.cvs",
                ".SVN/",
                "root/.Svn/entries",
                ".CVS/",
                "root/cVS/entries",
                "cvsignore/",
                "cvsignore",
                ".cvsignore/1.txt",
                "root/.cvsignore/" };

        for (String filename : params) {
            if (!instance.accept(filename)) {
                fail("did not accept filename: " + filename);
            }
        }
    }

    public void testFailed() {
        final String[] params = { "CVS/",
                ".svn/",
                "root/CVS/",
                "root/.svn/",
                "root/folder1/CVS/base",
                "root/folder1/.svn/base",
                ".cvsignore",
                "root/.cvsignore" };

        for (String filename : params) {
            if (instance.accept(filename)) {
                fail("accepted filename: " + filename);
            }
        }
    }
}
