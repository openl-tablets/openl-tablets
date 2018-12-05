/*
 * Created on Jul 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenClassHolder;
import org.openl.types.impl.AOpenSchema;
import org.openl.util.ASelector;
import org.openl.util.IConvertor;
import org.openl.util.IOpenIterator;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.tree.FileTreeIterator;

/**
 * @author snshor
 *
 */
public class JavaOpenSchema extends AOpenSchema {

    static class ClassNameSelector extends ASelector<String> {

        public boolean select(String name) {
            return name.endsWith(".class");
        }

    }

    static class FileNameToClassCollector implements IConvertor<String, String> {
        int rootlength;
        char separator;

        FileNameToClassCollector(int rootlength, char separator) {
            this.rootlength = rootlength; // adjust for last "/" or "\"
            this.separator = separator;
        }

        public String convert(String s) {
            s = s.substring(rootlength, s.length() - 6); // ends with
            // ".class"

            int len = s.length();
            StringBuilder buf = new StringBuilder(len);

            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                if (c == separator) {
                    buf.append('.');
                } else {
                    buf.append(c);
                }
            }

            return buf.toString();

        }

    }

    static class JavaOpenClassHolder implements IOpenClassHolder {
        ClassLoader classLoader;
        String className;
        IOpenClass javaOpenClass;

        JavaOpenClassHolder(String className, ClassLoader classLoader) {
            this.className = className;
            this.classLoader = classLoader;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.base.INamedThing#getDisplayName(int)
         */
        public String getDisplayName(int mode) {
            return javaOpenClass.getDisplayName(mode);
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.base.INamedThing#getName()
         */
        public String getName() {
            return className;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenClassHolder#getOpenClass()
         */
        public IOpenClass getOpenClass() {
            try {
                if (javaOpenClass == null) {
                    javaOpenClass = JavaOpenClass.getOpenClass(classLoader.loadClass(className));
                }
                return javaOpenClass;
            } catch (Exception ex) {
                throw RuntimeExceptionWrapper.wrap(ex);
            }
        }

    }

    static final ISelector<String> CLASSFILENAME_SELECTOR = new ClassNameSelector();

    String[] classpath;

    ClassLoader classLoader;

    public JavaOpenSchema(String[] classpath, ClassLoader classLoader) {
        this.classpath = classpath;
        this.classLoader = classLoader;

    }

    @Override
    protected Map<String, IOpenClassHolder> buildAllClasses() {
        HashMap<String, IOpenClassHolder> map = new HashMap<String, IOpenClassHolder>();

        for (String cp : classpath) {
            try {
                for (Iterator<String> iter = getIterator(cp); iter.hasNext(); ) {
                    String className = iter.next();
                    map.put(className, new JavaOpenClassHolder(className, classLoader));
                }
            } catch (Exception ex) {
                throw RuntimeExceptionWrapper.wrap(ex);
            }
        }

        return map;
    }

    public String[] getClasspath() {
        return classpath;
    }

    protected Iterator<String> getDirectoryIterator(String dirname) throws Exception {

        IConvertor<File, String> fileToStringCollector = new IConvertor<File, String>() {

            public String convert(File f) {
                return f.getAbsolutePath();
            }
        };

        File dir = new File(dirname).getCanonicalFile();

        String dirName = dir.getAbsolutePath();

        return new FileTreeIterator(dir, 0).collect(fileToStringCollector).select(CLASSFILENAME_SELECTOR).collect(
                new FileNameToClassCollector(dirName.length() + 1, File.separatorChar));
    }

    protected Iterator<String> getIterator(String classPathComponent) throws Exception {
        // determine a type of classpath component
        // it will be either .jar, .zip, .war files or directory
        // TODO if this is URL not a file we need to have a mechanism to deal
        // with it

        if (classPathComponent.endsWith(".jar") || classPathComponent.endsWith(".zip")) {
            return getJarOrZipIterator(classPathComponent);
        } else if (classPathComponent.endsWith(".war")) {
            // TODO .war support
            throw new UnsupportedOperationException(".war archives are not supported yet");
        } else {
            return getDirectoryIterator(classPathComponent);
        }

    }

    @SuppressWarnings("unchecked")
    protected Iterator<String> getJarOrZipIterator(String jarname) throws Exception {
    	
        ZipFile zip = new ZipFile(jarname);
    	try
    	{

        IConvertor<ZipEntry, String> zipToStringCollector = new IConvertor<ZipEntry, String>() {
            public String convert(ZipEntry zipentry) {
                return zipentry.getName();
            }
        };

        IOpenIterator<ZipEntry> entries = (IOpenIterator<ZipEntry>) OpenIterator.fromEnumeration(zip.entries());

        return entries.collect(zipToStringCollector).select(CLASSFILENAME_SELECTOR).collect(
                new FileNameToClassCollector(0, File.separatorChar));
    	}
    	finally
    	{
    		zip.close();
    	}

    }

    public void setClasspath(String[] string) {
        classpath = string;
    }
}
