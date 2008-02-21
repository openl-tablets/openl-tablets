package org.openl.rules.workspace.abstracts;

import java.util.Collection;

public interface ProjectFolder extends ProjectArtefact {
    Collection<? extends ProjectArtefact> getArtefacts();
}
