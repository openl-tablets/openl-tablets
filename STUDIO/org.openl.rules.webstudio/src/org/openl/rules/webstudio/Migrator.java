package org.openl.rules.webstudio;

import java.io.IOException;
import java.util.HashMap;

import org.openl.info.OpenLVersion;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For setting migration purposes. It cleans up default settings and reconfigure user defined properties.
 * 
 * @author Yury Molchan
 */
public class Migrator {
    private static final Logger LOG = LoggerFactory.getLogger(Migrator.class);

    public static void migrate() {
        HashMap<String, String> props = new HashMap<>();
        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        props.put("project.history.unlimited", null); // Remove
        props.put(".version", OpenLVersion.getVersion()); // Mark the file version
        try {
            DynamicPropertySource settings = DynamicPropertySource.get();
            settings.save(props);
            settings.reloadIfModified();
        } catch (IOException e) {
            LOG.error("Migration of properties failed.", e);
        }
    }
}
