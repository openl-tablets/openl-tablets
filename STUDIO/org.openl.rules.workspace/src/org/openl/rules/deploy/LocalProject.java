package org.openl.rules.deploy;

import java.util.Map;

import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.abstraction.IProjectArtefact;
import org.openl.rules.repository.api.FileData;

public class LocalProject extends LocalProjectFolder implements IProject {

    public LocalProject(FileData data, Map<String, IProjectArtefact> artefacts) {
        super(data, artefacts);
    }

    @Override
    public boolean isDeleted() {
        return getData().isDeleted();
    }

}
