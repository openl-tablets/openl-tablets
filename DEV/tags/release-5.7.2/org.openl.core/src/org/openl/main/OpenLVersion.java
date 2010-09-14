/**
 * Created Oct 26, 2006
 */
package org.openl.main;

import java.util.Properties;

import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public class OpenLVersion {
    static final public String prop_file_name = "openl.version.properties";
    static final public String prop_version_name = "openl.version";
    static final public String prop_build_name = "openl.build";
    static final public String prop_url_name = "openl.url";
    static final public String prop_year_name = "openl.copyrightyear";

    static Properties props = null;

    public static String getBuild() {
        return getProperties().getProperty(prop_build_name, "??");
    }

    public static String getCopyrightYear() {
        return getProperties().getProperty(prop_year_name, "??");
    }

    static synchronized Properties getProperties() {
        if (props == null) {
            props = new Properties();

            try {
                props.load(OpenLVersion.class.getResourceAsStream(prop_file_name));
            } catch (Throwable t) {
                Log.warn(prop_file_name + " not found", t);
            }
        }

        return props;
    }

    public static String getURL() {
        return getProperties().getProperty(prop_url_name, "??");
    }

    public static String getVersion() {
        return getProperties().getProperty(prop_version_name, "???");
    }

}
