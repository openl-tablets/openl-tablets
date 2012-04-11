package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrOldProject extends JcrOldEntity implements RProject {


    private JcrOldFolder rootFolder;
    private JcrDependencies dependencies;

    public JcrOldProject(String name, Node node, CommonVersion version) throws RepositoryException {
        super(null, name, node);
        checkNodeType(JcrNT.NT_PROJECT);

        Node files = NodeUtil.normalizeOldNode(node.getNode(JcrProject.NODE_FILES), version);
        rootFolder = new JcrOldFolder(this, null, files, version);

        Node deps = node.getNode(JcrProject.NODE_DEPENDENCIES);
        dependencies = new JcrDependencies(deps);
    }

    public void commit(CommonUser user) throws RRepositoryException {
        notSupported();
    }

    public void delete(CommonUser user) throws RRepositoryException {
        notSupported();
    }

    public void erase(CommonUser user) throws RRepositoryException {
        notSupported();
    }

    public Collection<ProjectDependency> getDependencies() throws RRepositoryException {
        return dependencies.getDependencies();
    }

    public RProject getProjectVersion(CommonVersion version) throws RRepositoryException {
        throw new RRepositoryException("In versioned mode can work with one version only!", null);
    }

    public RFolder getRootFolder() {
        return rootFolder;
    }

    public boolean isMarked4Deletion() throws RRepositoryException {
        // not supported
        return false;
    }

    public void riseVersion(int major, int minor) throws RRepositoryException {
        notSupported();
    }

    public void setDependencies(Collection<? extends ProjectDependency> dependencies) throws RRepositoryException {
        notSupported();
    }

    public void undelete(CommonUser user) throws RRepositoryException {
        notSupported();
    }

    //TODO Fixme
    public RFile createFile(String name) throws RRepositoryException {
        notSupported();
        return null;
    }

    public RFolder createFolder(String name) throws RRepositoryException {
        notSupported();
        return null;
    }

    public List<RFile> getFiles() throws RRepositoryException {
        return rootFolder.getFiles();
    }

    public List<RFolder> getFolders() throws RRepositoryException {
        return rootFolder.getFolders();
    }
}
