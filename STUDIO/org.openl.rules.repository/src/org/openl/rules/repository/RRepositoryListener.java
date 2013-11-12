package org.openl.rules.repository;

public interface RRepositoryListener {
    public static class RRepositoryEvent {
        private String projectName;

        public RRepositoryEvent(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectName() {
            return projectName;
        }
    }
    
    void onEventInRulesProjects(RRepositoryEvent event);
    void onEventInDeploymentProjects(RRepositoryEvent event);
}
