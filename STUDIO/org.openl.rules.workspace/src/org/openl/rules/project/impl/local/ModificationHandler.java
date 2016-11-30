package org.openl.rules.project.impl.local;

public interface ModificationHandler {
    void notifyModified(String path);

    boolean isModified(String path);

    void clearModifyStatus(String path);

    boolean isMarkerFile(String name);
}
