package org.openl.rules.project.impl.local;

public interface ProjectState {
    void notifyModified();

    boolean isModified();

    void clearModifyStatus();

    void setProjectVersion(String version);

    String getProjectVersion();
}
