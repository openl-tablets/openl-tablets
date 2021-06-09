package org.openl.rules.deploy;

import java.util.Collections;
import java.util.Map;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.IProjectArtefact;
import org.openl.rules.project.abstraction.IProjectFolder;
import org.openl.rules.repository.api.FileData;
import org.openl.util.FileUtils;

public class LocalProjectFolder extends ALocalProjectArtefact implements IProjectFolder {

    private final FileData data;
    private final Map<String, IProjectArtefact> artefacts;

    public LocalProjectFolder(FileData data, Map<String, IProjectArtefact> artefacts) {
        super(FileUtils.getName(data.getName()));
        this.data = data;
        this.artefacts = Collections.unmodifiableMap(artefacts);
    }

    @Override
    public String getFolderPath() {
        return data.getName();
    }

    @Override
    public IProjectArtefact getArtefact(String name) throws ProjectException {
        return artefacts.get(name);
    }

    @Override
    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFolderPath());
    }

    public FileData getData() {
        return data;
    }

}
