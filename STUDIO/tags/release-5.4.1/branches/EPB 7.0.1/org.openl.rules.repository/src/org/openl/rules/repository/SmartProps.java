package org.openl.rules.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Smart Properties.
 * It automatically solves issues around properties files.
 * Thus, it removes extra spaces...
 * 
 * @author Aleh Bykhavets
 *
 */
//TODO: find 'ready solution'. It's better to 'reuse' instead of 'reinvent'.
public class SmartProps {
    private Properties props;

    /**
     * Create wrapper for Properties.
     *
     * @param fullName full name or path in class paths to a properties file
     */
    public SmartProps(String fullName) {
        InputStream is = getPropertyStream(fullName);

        if (is == null) {
            throw new IllegalArgumentException("Cannot find resource by name '" + fullName + "'");
        }

        props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            System.err.println("* Failed to load properties: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                System.err.println("* Failed to close InputStream: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private InputStream getPropertyStream(String fullName) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(fullName);

        if (is == null) {
            String catalinaBase = System.getProperty("catalina.base");
            if (catalinaBase != null) {
                try {
                    return new FileInputStream(new File(catalinaBase, fullName));
                } catch (FileNotFoundException e) {}
            }
        }
        
        return is;
    }

    /**
     * Create wrapper for <code>Properties</code>.
     *
     * @param properties properties to initialize this instance from
     */
    public SmartProps(Properties properties) {
        props = (Properties) properties.clone();
    }
    

    /**
     * Gets string value of property by name.
     *
     * @param name name of property
     * @return value of property
     */
    public String getStr(String name) {
        String s  = props.getProperty(name);

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
     * @return value of property or default value if property is <code>null</code> or epmty string
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
