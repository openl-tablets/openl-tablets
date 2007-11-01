package org.openl.rules.lw;

import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;

public interface LocalProjectArtefact extends ProjectArtefact {
    LocalProjectArtefact getArtefact(String name) throws ProjectException;

    boolean isNew();
    boolean isChanged();

    void refresh();
    void remove();
}
