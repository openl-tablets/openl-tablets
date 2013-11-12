package org.openl.rules.workspace.abstracts;

import java.util.EventListener;

public interface ProjectContainerListener extends EventListener {
    void afterRelease(ProjectsContainer projectsContainer);
}
