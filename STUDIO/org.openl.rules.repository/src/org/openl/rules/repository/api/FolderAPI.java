package org.openl.rules.repository.api;

import java.io.InputStream;
import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;

public interface FolderAPI extends ArtefactAPI {
    ArtefactAPI getArtefact(String name) throws ProjectException;

    boolean hasArtefact(String name);

    FolderAPI addFolder(String name) throws ProjectException;

    ResourceAPI addResource(String name, InputStream content) throws ProjectException;

    Collection<? extends ArtefactAPI> getArtefacts();
    @Override
    FolderAPI getVersion(CommonVersion version) throws ProjectException;
}
