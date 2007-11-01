package org.openl.rules.commons.projects;

import java.util.Collection;

public interface ProjectFolder extends ProjectArtefact {
    Collection<? extends ProjectArtefact> getArtefacts();
}
