package org.openl.rules.project.abstraction;

public final class Comments {
    private Comments() {
    }

    public static String createProject(String projectName) {
        return "Create project " + projectName;
    }

    public static String archiveProject(String projectName) {
        return "Archive project " + projectName;
    }

    public static String restoreProject(String projectName) {
        return "Restore project " + projectName;
    }

    public static String eraseProject(String projectName) {
        return "Erase project " + projectName;
    }
}
