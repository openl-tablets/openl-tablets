package org.openl.rules.repository;

import java.io.InputStream;

import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Repository. A Repository can have any number of OpenL Rules
 * Projects.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RRepository {

    /**
     * Releases resources allocated by this Rules Repository instance.
     */
    void release();


    //TODO new method names and comments


    void setListener(Listener listener);

    ArtefactAPI getArtefact(String name) throws RRepositoryException;

    ResourceAPI createResource(String name, InputStream inputStream) throws RRepositoryException;

    ArtefactAPI rename(String path, String destination) throws RRepositoryException;
}
