package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;

public interface LocalProjectArtefact extends ProjectArtefact {
    LocalProjectArtefact getArtefact(String name) throws ProjectException;

    boolean isNew();
    boolean isChanged();

    void refresh();
    void remove();
}
