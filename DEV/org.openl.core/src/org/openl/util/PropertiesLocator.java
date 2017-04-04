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
import java.net.URL;
import java.util.Properties;

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
}