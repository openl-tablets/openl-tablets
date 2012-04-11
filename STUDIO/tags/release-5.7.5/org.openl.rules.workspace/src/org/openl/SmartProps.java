package org.openl;

import org.openl.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Smart Properties. It automatically solves issues around properties files.
 * Thus, it removes extra spaces...
 * <p>
 * If properties file cannot be opened or I/O error happens, SmartProps reports
 * error in log and continue execution. Thus, it is better to use getters with
 * default values.
 *
 * @author Aleh Bykhavets
 *
 */
// TODO: find 'ready solution'. It's better to 'reuse' instead of 'reinvent'.
public class SmartProps {
    private final Properties props;

    /**
     * Create wrapper for <code>Properties</code>.
     *
     * @param properties properties to initialize this instance from
     */
    public SmartProps(Properties properties) {
        props = (Properties) properties.clone();
    }

    /**
     * Create wrapper for Properties.
     *
     * @param fullName full name or path in class paths to a properties file
     */
    public SmartProps(String fullName) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(fullName);

        if (is == null) {
            Log.error("Cannot find resource by name ''{0}''!", fullName);
        }

        props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            Log.error("Failed to load properties!", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.error("Failed to close InputStream!", e);
            }
        }
    }

    /**
     * Gets string value of property by name.
     *
     * @param name name of property
     * @return value of property
     */
    public String getStr(String name) {
        String s = props.getProperty(name);

        if (s != null) {
            // extra spaces is a big problem
            s = s.trim();
        }

        return s;
    }

    /**
     * Gets string value of property by name.
     *
     * @param name name of property
     * @param def default value
     * @return value of property or default value if property is
     *         <code>null</code> or epmty string
     */
    public String getStr(String name, String def) {
        String s = getStr(name);

        if (s == null || s.length() == 0) {
            // use default value istead of null or empty string
            s = def;
        }

        return s;
    }
}
