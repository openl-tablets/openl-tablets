package org.openl.rules.workspace.abstracts;

import java.util.EventListener;

public interface ProjectContainerListener<T extends Project> extends EventListener {
    void afterRelease(ProjectsContainer<T> projectsContainer);
}
