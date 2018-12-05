/*
 * Created on Jul 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openl.OpenL;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.tree.FileTreeIterator;

public class ClassLoaderFactory {

    public static ClassLoader createClassLoader(String cp,
            ClassLoader parent,
            String userHome) throws Exception {
        String[] classpath = splitClassPath(cp);
        List<URL> urls = new ArrayList<URL>();
        for (int i = 0; i < classpath.length; i++) {

            if (classpath[i].endsWith("*")) {
                makeWildcardPath(makeFile(userHome, classpath[i].substring(0, classpath[i].length() - 1)), urls);
            } else {

                File f = makeFile(userHome, classpath[i]);

                if (!f.exists()) {
                    throw new IOException("File " + f.getPath() + " does not exist");
                }

                urls.add(makeFile(userHome, classpath[i]).toURI().toURL());
            }

        }

        URL[] uurl = urls.toArray(new URL[urls.size()]);
        return new URLClassLoader(uurl, parent);
    }

    public static ClassLoader getOpenlCoreClassLoader(ClassLoader ucl) {
        try {
            Class<?> c = ucl.loadClass(OpenL.class.getName());
            if (c != null) {
                return ucl;
            }
        } catch (Exception e) {
        }
        return OpenL.class.getClassLoader();
    }

    private static File makeFile(String root, String name) throws Exception {
        File f = new File(name);

        if (f.isAbsolute() || name.startsWith("/")) {
            return f.getCanonicalFile();
        }

        return new File(root, name).getCanonicalFile();

    }

    private static void makeWildcardPath(File root, List<URL> urls) {

        ISelector<File> sel = new ASelector<File>() {
            public boolean select(File f) {
                String apath = f.getAbsolutePath();
                boolean res = apath.endsWith(".jar") || apath.endsWith(".zip");
                return res;
            }

        };

        Iterator<File> iter = new FileTreeIterator(root, 0).select(sel);

        for (; iter.hasNext();) {
            File f = iter.next();
            try {
                urls.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

    }

    private static String[] splitClassPath(String classpath) {
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

        String[] res = new String[st.countTokens()];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

}
