package org.openl.rules.webstudio.web.repository.event;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.context.ApplicationEvent;

public class ProjectDeletedEvent extends ApplicationEvent {

    public ProjectDeletedEvent(AProjectArtefact project) {
        super(project);
    }

    public AProjectArtefact getProject() {
        return (AProjectArtefact) getSource();
    }
}
