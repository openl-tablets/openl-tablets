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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import org.openl.OpenL;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.tree.FileTreeIterator;

/**
 * @author snshor
 *
 */
public class ClassLoaderFactory {

    // public ClassLoaderFactory()
    // {
    // // userClassLoaders.put("org.openl.core", getOpenlCoreLoader());
    // }

    static final class Key {
        String name;
        String classpath;
        ClassLoader parent;
        IUserContext cxt;

        Key(String name, String classpath, ClassLoader parent, IUserContext cxt) {
            this.name = name;
            this.classpath = classpath;
            this.parent = parent;
            this.cxt = cxt;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key k = (Key) obj;

            return Objects.equals(classpath, k.classpath) &&
                    Objects.equals(cxt, k.cxt) &&
                    Objects.equals(parent, k.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, cxt, classpath);
        }

    }

    static HashMap<Key, ClassLoader> userClassLoaders = new HashMap<Key, ClassLoader>();

    static public ClassLoader createClassLoader(String classpath, ClassLoader parent, IUserContext ucxt)
            throws Exception {

        return createClassLoader(splitClassPath(classpath), parent, ucxt);
    }
    
    public static ClassLoader createClassLoader(String classpath, ClassLoader parent, String userHome) throws Exception {
        return createClassLoader(splitClassPath(classpath), parent, userHome);
    }
    
    public static ClassLoader createClassLoader(String[] classpath, ClassLoader parent, String userHome)throws Exception {
        List<URL> urls = new ArrayList<URL>();
        for (int i = 0; i < classpath.length; i++) {

            if (classpath[i].endsWith("*")) {
                makeWildcardPath(makeFile(userHome, classpath[i].substring(0, classpath[i].length() - 1)),
                        urls);
            } else {

                File f = makeFile(userHome, classpath[i]);

                if (!f.exists()) {
                    throw new IOException("File " + f.getPath() + " does not exist");
                }

                urls.add(makeFile(userHome, classpath[i]).toURI().toURL());
            }

            // System.out.println(urls[i].toExternalForm());
        }

        URL[] uurl = urls.toArray(new URL[urls.size()]);
        return new URLClassLoader(uurl, parent);
    }

    static public ClassLoader createClassLoader(String[] classpath, ClassLoader parent, IUserContext ucxt)
            throws Exception {
        return createClassLoader(classpath, parent, ucxt.getUserHome());
    }

    public static synchronized ClassLoader createUserClassloader(String name, String classpath, ClassLoader parent,
            IUserContext ucxt) throws Exception {

        Log.debug("name=" + name + " cp=" + classpath + " " + ucxt + " cl=" + parent);

        Key key = new Key(name, classpath, parent, ucxt);
        ClassLoader loader = userClassLoaders.get(key);

        Log.debug(loader == null ? "New" : "Old");

        if (loader == null) {
            loader = createClassLoader(classpath, parent, ucxt);
            // TODO fix it
            userClassLoaders.put(key, loader);
        }

        return loader;
    }

    public static ClassLoader getOpenlCoreLoader(ClassLoader ucl) {
        try {
            Class<?> c = ucl.loadClass(OpenL.class.getName());
            if (c != null) {
                return ucl;
            }

        } catch (Exception e) {
        }

        return OpenL.class.getClassLoader();
    }

    static File makeFile(String root, String name) throws Exception {
        File f = new File(name);

        if (f.isAbsolute() || name.startsWith("/")) {
            return f.getCanonicalFile();
        }

        return new File(root, name).getCanonicalFile();

    }

    /**
     * @param string
     * @param string2
     * @param v
     */
    public static void makeWildcardPath(File root, List<URL> urls) {

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

    //FIXME: multithreading issue: users can reset foreign OpenL calculation
    public static synchronized HashMap<Key, ClassLoader> reset() {
        HashMap<Key, ClassLoader> oldLoaders = userClassLoaders;

        userClassLoaders = new HashMap<Key, ClassLoader>();

        return oldLoaders;
    }

    static protected String[] splitClassPath(String classpath) {
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

        String[] res = new String[st.countTokens()];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }
   
}
