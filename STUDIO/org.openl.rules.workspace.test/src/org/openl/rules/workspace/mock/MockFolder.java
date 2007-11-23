package org.openl.rules.workspace.mock;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectFolder;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;

public class MockFolder extends MockArtefact implements ProjectFolder{
    private Collection<ProjectArtefact> artefacts;

    public MockFolder(String name, MockFolder parent) {
        super(name, parent);
    }

    public Collection<? extends ProjectArtefact> getArtefacts() {
        return artefacts == null ? Collections.EMPTY_LIST : artefacts;
    }

    public void setArtefacts(Collection<ProjectArtefact> artefacts) {
        this.artefacts = artefacts;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    public MockArtefact add(MockArtefact artefact) {
        if (artefacts == null) {
            artefacts = new ArrayList<ProjectArtefact>();
        }
        artefacts.add(artefact);
        return artefact;
    }

    public MockFolder addFolder(String artefactName) {
        MockFolder folder = new MockFolder(artefactName, this);
        add(folder);
        return folder;
    }

    public MockResource addFile(String artefactName) {
        return (MockResource) add(new MockResource(artefactName, this));
    }
}
