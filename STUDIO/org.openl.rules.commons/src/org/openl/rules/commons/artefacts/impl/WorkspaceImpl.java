package org.openl.rules.commons.artefacts.impl;

import org.openl.rules.commons.artefacts.*;

import java.util.Collection;
import java.util.LinkedList;

public class WorkspaceImpl extends ArtefactImpl implements Workspace {

    public WorkspaceImpl(String name) throws ArtefactException {
        super(name, null);
    }

    /** {@inheritDoc} */
    public boolean hasProject(String name) {
        return hasArtefact(name);
    }

    /** {@inheritDoc} */
    public Project getProject(String name) throws ArtefactException {
        return (Project) getArtefact(name);
    }

    /** {@inheritDoc} */
    public Collection<Project> getProjects() throws ArtefactException {
        Collection<Project> projects = new LinkedList<Project>();
        for (Artefact a : getArtefacts()) {
            projects.add((Project) a);
        }

        return projects;
    }

    /** {@inheritDoc} */
    public ArtefactPath getArtefactPath(Artefact artefact) throws ArtefactException {
        LinkedList<String> segments = new LinkedList<String>();

        Artefact a = artefact;
        while ((a != null) && (a != this)) {
            segments.addFirst(a.getName());
            a = a.getParent();
        }

        return new ArtefactPathImpl(segments);
    }

    /** {@inheritDoc} */
    public Artefact findArtefactByPath(ArtefactPath artefactPath) throws ArtefactException {
        Artefact result = this;

        for (int i = 0; i < artefactPath.segmentCount(); i++) {
            String segment = artefactPath.segment(i);

            if (result.hasArtefact(segment)) {
                // deeper and deeper
                result = result.getArtefact(segment);
            } else {
                // failed to find
                result = null;
                break;
            }
        }

        return result;
    }
}
