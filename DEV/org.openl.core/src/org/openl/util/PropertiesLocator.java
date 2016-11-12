/*
 * Created on Dec 3, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.IConfigurableResourceContext;

/**
 * @author snshor
 */
public class PropertiesLocator {

    public static String findPropertyValue(String propertyName, String propertyFileName,
            IConfigurableResourceContext ucxt) {

        URL url = ucxt.findClassPathResource(propertyFileName);
        if (url != null) {
            InputStream is = null;
            try {
                is = url.openStream();
                Properties p = new Properties();
                p.load(is);
                return p.getProperty(propertyName);
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable t) {
                    Log.error("Error closing stream", t);
                }
            }
        }

        File f = ucxt.findFileSystemResource(propertyFileName);
        if (f != null) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                Properties p = new Properties();
                p.load(is);
                return p.getProperty(propertyName);
            } catch (IOException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable t) {
                    Log.error("Error closing stream", t);
                }
            }
        }

        return ucxt.findProperty(propertyName);

    }

    public static String locateFileOrURL(String fileName) {
        return locateFileOrURL(fileName, Thread.currentThread().getContextClassLoader(), new String[] { "." });
    }

    public static String locateFileOrURL(String fileName, ClassLoader cl, String[] fileRoots) {
        ConfigurableResourceContext cxt = new ConfigurableResourceContext(cl, fileRoots);
        return locateFileOrURL(fileName, cxt);
    }

    private static String locateFileOrURL(String fileName, IConfigurableResourceContext ucxt) {
        File f = ucxt.findFileSystemResource(fileName);
        if (f != null) {
            return f.getAbsolutePath();
        }

        URL url = ucxt.findClassPathResource(fileName);
        if (url != null) {
            return url.toExternalForm();
        }

        try {
            url = new URL(fileName);
            return fileName;

        } catch (MalformedURLException e) {
        }

        return null;
    }

    public static URL locateToURL(String fileName) {
        return locateToURL(fileName, Thread.currentThread().getContextClassLoader(), new String[] { "." });
    }

    public static URL locateToURL(String fileName, ClassLoader cl, String[] fileRoots) {
        ConfigurableResourceContext cxt = new ConfigurableResourceContext(cl, fileRoots);
        return locateToURL(fileName, cxt);
    }

    private static URL locateToURL(String fileName, IConfigurableResourceContext ucxt) {
        File f = ucxt.findFileSystemResource(fileName);
        if (f != null) {
            try {
                return f.toURI().toURL();
            } catch (MalformedURLException e) {
            }
        }

        URL url = ucxt.findClassPathResource(fileName);
        if (url != null) {
            return url;
        }

        try {
            return new URL(fileName);
        } catch (MalformedURLException e) {
        }
        return null;
    }

}