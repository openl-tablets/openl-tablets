/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openl.util.tree.FileTreeIterator;
import org.openl.util.tree.TreeIterator;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TreeIteratorTest extends TestCase {

    static class Counter {
        int files;
        int lines;
    }
    static class XTreeAdaptor implements TreeIterator.TreeAdaptor {
        public Iterator children(Object node) {
            if (node.getClass().isArray()) {
                return OpenIterator.fromArray((Object[]) node);
            }
            return null;
        }
    }
    static int count = 0;

    String[] x1 = { "aaa", "bbb", "ccc" };

    String[] x2 = { "ddd", "eee", "fff" };

    String[] x3 = { "ggg", "hhh" };

    String[][] root = { x1, x2, x3 };

    static void findInZip(String zipName, final String fnameEnd) throws IOException {
        ZipFile zip = new ZipFile(zipName);

        IOpenIterator it = OpenIterator.fromEnumeration(zip.entries());

        ISelector sel = new ASelector() {
            public boolean select(Object obj) {
                ZipEntry ze = (ZipEntry) obj;
                boolean res = ze.getName().indexOf(fnameEnd) >= 0;
                if (res) {
                    System.out.println("  " + ze.getName());
                }
                return res;
            }

        };

        count += it.select(sel).count();
    }

    public static void main(String[] args) throws Exception {
        String fname = "c:/3p/jakarta-tomcat-5.0.25";
        String suffix = ".jar";
        String x = "catalina/con";

        FileTreeIterator it = new FileTreeIterator(new File(fname), 0);

        for (; it.hasNext();) {
            File f = it.next();
            if (f.getName().endsWith(suffix)) {
                String jar = f.getCanonicalPath();
                System.out.println(jar);
                findInZip(jar, x);
            }
        }
        System.out.println("Total found: " + count);
    }

    /**
     * Constructor for TreeIteratorTest.
     *
     * @param arg0
     */
    public TreeIteratorTest(String arg0) {
        super(arg0);
    }

    void countFilesAndLines(Counter cnt, String root, ISelector selector) throws IOException {
        FileTreeIterator fti = new FileTreeIterator(new File(root), 0);

        for (; fti.hasNext();) {
            File ff = fti.nextFile();
            if (selector.select(ff)) {
                ++cnt.files;
                cnt.lines += countLines(ff);
            }
        }

    }

    private int countLines(File ff) throws IOException {
        int lines = 0;

        FileReader fr = new FileReader(ff);
        BufferedReader br = new BufferedReader(fr);
        while (br.readLine() != null) {
            ++lines;
        }

        return lines;
    }

    TreeIterator create(int mode) {
        return new TreeIterator(root, new XTreeAdaptor(), mode);
    }

    public void testCount() {
        TreeIterator it = create(TreeIterator.DEFAULT);
        Assert.assertEquals(12, it.count());
    }

    public void testFile() throws Exception {
        FileTreeIterator it = new FileTreeIterator(new File(".").getCanonicalFile(), 0);

        ISelector sel = new ASelector() {
            public boolean select(Object obj) {
                File f = (File) obj;
                boolean res = f.getAbsolutePath().endsWith("TreeIteratorTest.java");
                Log.info(f.getAbsolutePath());
                if (res) {
                    Log.info(f.getAbsolutePath());
                }
                return res;
            }

        };

        Assert.assertEquals(it.select(sel).count(), 1);

    }

    /*
     * Test for int size()
     */
    public void testSize() {
        TreeIterator it = create(TreeIterator.DEFAULT);
        Assert.assertEquals(-1, it.size());

    }

    public void testZip() throws Exception {

        URL url = this.getClass().getClassLoader().getResource("org/openl/util/util.zip");
        ZipFile zip = new ZipFile(url.toURI().getPath());

        IOpenIterator it = OpenIterator.fromEnumeration(zip.entries());

        ISelector sel = new ASelector() {
            public boolean select(Object obj) {
                ZipEntry ze = (ZipEntry) obj;
                boolean res = ze.getName().endsWith("TreeIteratorTest.java");
                Log.info(ze.getName());
                return res;
            }

        };

        Assert.assertEquals(it.select(sel).count(), 1);

    }

}
