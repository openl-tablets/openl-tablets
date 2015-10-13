package org.openl.extension.xmlrules;

import java.util.HashSet;
import java.util.Set;

public class ProjectData {
    private static final ThreadLocal<ProjectData> INSTANCE = new ThreadLocal<ProjectData>();

    public static ProjectData getCurrentInstance() {
        ProjectData projectData = INSTANCE.get();

        if (projectData == null) {
            projectData = new ProjectData();
            INSTANCE.set(projectData);
        }

        return projectData;
    }

    public static void removeCurrentInstance() {
        INSTANCE.remove();
    }

    private final Set<String> types = new HashSet<String>();
    private final Set<String> fields = new HashSet<String>();

    public Set<String> getTypes() {
        return types;
    }

    public Set<String> getFields() {
        return fields;
    }
}
