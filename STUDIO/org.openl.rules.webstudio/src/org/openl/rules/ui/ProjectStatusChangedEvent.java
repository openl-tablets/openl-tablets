package org.openl.rules.ui;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * Published by {@link ProjectModel} whenever the state observable through the project
 * status API might have changed: a new compile cycle started, a module within the
 * current cycle finished compiling, or the cycle terminated. Listeners are expected to
 * re-derive the status from the current model state rather than from event-specific
 * payloads; the event itself just signals "re-render".
 *
 * @author Vladyslav Pikus
 */
@Getter
public class ProjectStatusChangedEvent extends ApplicationEvent {

    private final RulesProject project;
    private final String userName;

    public ProjectStatusChangedEvent(ProjectModel source, RulesProject project, String userName) {
        super(source);
        this.project = project;
        this.userName = userName;
    }

    public ProjectModel getProjectModel() {
        return (ProjectModel) getSource();
    }
}
