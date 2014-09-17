package org.openl.rules.workspace.dtr;

import java.util.EventListener;

public interface DesignTimeRepositoryListener extends EventListener {
    class DTRepositoryEvent {
        private String projectName;

        public DTRepositoryEvent(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectName() {
            return projectName;
        }
    }

    void onRulesProjectModified(DTRepositoryEvent event);

    void onDeploymentProjectModified(DTRepositoryEvent event);
}
