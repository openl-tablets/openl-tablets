package org.openl.rules.workspace.lw;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

public interface LocalProjectArtefact extends ProjectArtefact, RulesRepositoryArtefact {
    LocalProjectArtefact getArtefact(String name) throws ProjectException;

    boolean isChanged();

    boolean isNew();

    void refresh();

    void remove();
}
