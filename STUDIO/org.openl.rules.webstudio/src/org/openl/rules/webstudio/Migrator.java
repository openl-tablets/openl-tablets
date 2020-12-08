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
            props.put("repository.design.new-branch.pattern", migratedNewBranchPattern);
            props.put("repository.design.new-branch-pattern", null);
        }
        rename(settings, props, "repository.deploy-config.comment-validation-pattern", "repository.deploy-config.comment-template.comment-validation-pattern");
        rename(settings, props, "repository.deploy-config.invalid-comment-message", "repository.deploy-config.comment-template.invalid-comment-message");
        rename(settings, props, "repository.design.comment-validation-pattern", "repository.design.comment-template.comment-validation-pattern");
        rename(settings, props, "repository.design.invalid-comment-message", "repository.design.comment-template.invalid-comment-message");
        DynamicPropertySource.get().save(props);
    }

    private static void rename(DynamicPropertySource settings, HashMap<String, String> props, String oldKey, String newKey) {
        String value = (String) settings.getProperty(oldKey);
        props.put(oldKey, null);
        props.put(newKey, value);
    }
}
