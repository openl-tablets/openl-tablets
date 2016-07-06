/**
 * Created Oct 26, 2006
 */
package org.openl.main;

import java.io.InputStream;
import java.util.Properties;

import org.openl.util.IOUtils;
import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class OpenLVersion {

    public static final String PROP_FILE_NAME = "openl.version.properties";
    public static final String PROP_VERSION_NAME = "openl.version";
    public static final String PROP_URL_NAME = "openl.url";
    public static final String PROP_YEAR_NAME = "openl.build.date";

    private static Properties props = null;

    public static String getCopyrightYear() {
        return getProperties().getProperty(PROP_YEAR_NAME, "????").substring(0, 4);
    }

    static synchronized Properties getProperties() {
        if (props == null) {
            props = new Properties();
            
            InputStream propertiesFile = null;
            try {
            	propertiesFile = OpenLVersion.class.getResourceAsStream(PROP_FILE_NAME);
                props.load(propertiesFile);
            } catch (Throwable t) {
                Log.warn(PROP_FILE_NAME + " not found", t);
            } finally {
            	if (propertiesFile != null) {
            		IOUtils.closeQuietly(propertiesFile);
            	}
            }
        }

        return props;
    }

    public static String getURL() {
        return getProperties().getProperty(PROP_URL_NAME, "??");
    }

    public static String getVersion() {
        return getProperties().getProperty(PROP_VERSION_NAME, "???");
    }

}
