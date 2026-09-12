package org.openl.rules.ui;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * Published by {@link ProjectModel} whenever the state observable through the project
 * status API might have changed: a new compile cycle started, a module within the
 * current cycle finished compiling, or the cycle terminated. Listeners are expected to
 * re-derive the status from the current model state rather than from event-specific
 * payloads; the event itself just signals "re-render", and says how much of the state is
 * readable right now.
 *
 * @author Vladyslav Pikus
 */
@Getter
public class ProjectStatusChangedEvent extends ApplicationEvent {

    private final transient RulesProject project;
    private final String userName;

    /**
     * Set when only how far the compilation has come can be told.
     *
     * <p>A compilation holds the model from its first module to its last, and most of the status is read
     * under that same lock. An event raised while it is held carries what can be read without waiting for
     * it — the module counts and the names of the modules already built — and leaves the rest to the event
     * that follows the compilation.
     */
    private final boolean progressOnly;

    public ProjectStatusChangedEvent(ProjectModel source,
                                     RulesProject project,
                                     String userName,
                                     boolean progressOnly) {
        super(source);
        this.project = project;
        this.userName = userName;
        this.progressOnly = progressOnly;
    }

    public ProjectModel getProjectModel() {
        return (ProjectModel) getSource();
    }
}
