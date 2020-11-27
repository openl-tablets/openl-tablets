package org.openl.rules.webstudio;

import java.io.IOException;
import java.util.HashMap;

import org.openl.info.OpenLVersion;
import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DynamicPropertySource;

/**
 * For setting migration purposes. It cleans up default settings and reconfigure user defined properties.
 * 
 * @author Yury Molchan
 */
public class Migrator {

    public void migrate() throws IOException {
        DynamicPropertySource settings = DynamicPropertySource.get();
        if (!settings.getFile().exists()) {
            return;
        }
        HashMap<String, String> props = new HashMap<>();
        if (Props.bool("project.history.unlimited")) {
            props.put("project.history.count", ""); // Define unlimited
        }
        props.put("project.history.unlimited", null); // Remove
        props.put(".version", OpenLVersion.getVersion()); // Mark the file version
        // migrate design new-branch-pattern
        Object desNewBranchPattern = settings.getProperty("repository.design.new-branch-pattern");
        if (desNewBranchPattern != null) {
            String migratedNewBranchPattern = desNewBranchPattern.toString()
                    .replace("{0}", "{project-name}")
                    .replace("{1}", "{username}")
                    .replace("{2}", "{current-date}");
            props.put("repository.design.new-branch-pattern", migratedNewBranchPattern);
        }
        DynamicPropertySource.get().save(props);
    }
}
