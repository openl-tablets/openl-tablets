package org.openl.spring.env;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.EnumerablePropertySource;

import org.openl.util.ClassUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

/**
 * OpenL default property sources. Collects all openl-default.properties files.
 * <p>
 * Note: All openl-default.properties must contains unique keys.
 *
 * @author Yury Molchan
 */
public class DefaultPropertySource extends EnumerablePropertySource<Map<String, String>> {

    public static final String PROPS_NAME = "OpenL default properties";

    static final String OPENL_CONFIG_LOADED = "openl.config.loaded";

    private static final String OPENL_DEFAULT_PROPERTIES = "openl-default.properties";

    DefaultPropertySource() {
        super(PROPS_NAME, new HashMap<>());

        try {
            var classLoader = ClassUtils.getCurrentClassLoader(getClass());
            Enumeration<URL> resources = classLoader.getResources(OPENL_DEFAULT_PROPERTIES);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                PropertiesUtils.load(url, source::put);
                ConfigLog.LOG.info("+       Load: '{}'", url);
            }
        } catch (Exception e) {
            ConfigLog.LOG.error("!     Error:", e);
        }
        source.put(OPENL_CONFIG_LOADED, Boolean.TRUE.toString());
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> propertyNames = source.keySet();
        return propertyNames.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }

    public static void transferAllOpenLDefaultProperties(OutputStream out) throws IOException {
        writeLine(out, "#############################################################################################");
        writeLine(out, "###   ***  This file was generated!  ***                                                  ###");
        writeLine(out, "###   It is a composition of all default settings are used in the application.            ###");
        writeLine(out, "###   To customize settings, you can define it as an application.properties located at:   ###");
        writeLine(out, "###     * User's home directory:  ~/application.properties                                ###");
        writeLine(out, "###     * Working directory:      ./application.properties                                ###");
        writeLine(out, "###   Also all properties can be defined via:                                             ###");
        writeLine(out, "###     * Java system properties, like:                 -Dopenl.home=/home/openl          ###");
        writeLine(out, "###     * System environment variable as is:       export openl.home=/home/openl          ###");
        writeLine(out, "###     * or in the upper case with underscores:   export OPENL_HOME=/home/openl          ###");
        writeLine(out, "#############################################################################################");
        writeLine(out, "");
        var classLoader = ClassUtils.getCurrentClassLoader(DefaultPropertySource.class);
        Enumeration<URL> resources = classLoader.getResources(OPENL_DEFAULT_PROPERTIES);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();

            String path = url.toString();
            var pos = path.lastIndexOf("!/"); // Jar file delimiter
            pos = (pos < 0) ? path.length() : (pos - 1);
            pos = path.lastIndexOf('/', pos); // path separator
            path = path.substring(pos + 1);

            writeLine(out, "#-------------------------------------------------------------------------------------------#");
            writeLine(out, "### From: " + path);
            writeLine(out, "#-------------------------------------------------------------------------------------------#");
            try (var in = url.openStream()) {
                in.transferTo(out);
            }
            writeLine(out, "");
        }
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.UTF_8));
        out.write("\n".getBytes(StandardCharsets.UTF_8));
    }
}
