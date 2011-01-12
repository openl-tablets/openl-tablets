package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectDependency.ProjectDependencyHelper;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ArtefactProperties;

import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.AccessManager.check;
import static org.openl.rules.security.AccessManager.isGranted;

public class AProject extends AProjectFolder {
    protected CommonUser user;

    public AProject(FolderAPI api, CommonUser user) {
        super(api, null);
        this.user = user;
    }

    @Override
    public AProject getProject() {
        return this;
    }

    public List<ProjectDependency> getDependencies() {
        List<ProjectDependency> dependencies = new ArrayList<ProjectDependency>();
        if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
            InputStream content = null;
            try {
                content = ((AProjectResource) getArtefact(ArtefactProperties.DEPENDENCIES_FILE)).getContent();
                dependencies = ProjectDependencyHelper.deserialize(content);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(content);
            }
        }

        return dependencies;
    }

    public void setDependencies(List<ProjectDependency> dependencies) throws ProjectException {
        if (CollectionUtils.isEmpty(dependencies)) {
            if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
                getArtefact(ArtefactProperties.DEPENDENCIES_FILE).delete();
            }
        } else {
            String dependenciesAsString = ProjectDependencyHelper.serialize(dependencies);
            try {
                if (hasArtefact(ArtefactProperties.DEPENDENCIES_FILE)) {
                    ((AProjectResource) getArtefact(ArtefactProperties.DEPENDENCIES_FILE))
                            .setContent(new ByteArrayInputStream(dependenciesAsString.getBytes("UTF-8")));
                } else {
                    addResource(ArtefactProperties.DEPENDENCIES_FILE,
                            new ByteArrayInputStream(dependenciesAsString.getBytes("UTF-8")));
                }
            } catch (Exception e) {
                // TODO
                e.printStackTrace();
            }
        }
    }

    @Override
    public void delete() throws ProjectException {
        if (isLocked() && !isLockedByMe()) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user!", null,
                    getName());
        }

        check(PRIVILEGE_DELETE);

        if (isOpened()) {
            close();
        }
        if (isDeleted()) {
            throw new ProjectException("Project ''{0}'' is already marked for deletion!", null, getName());
        }

        try {
            addProperty(new PropertyImpl(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION, ValueType.BOOLEAN, true));
        } catch (PropertyException e) {
            throw new ProjectException("Failed to mark project as deleted.", e);
        }
    }

    public void checkIn() throws ProjectException {
        checkIn(user);
    }

    public void checkIn(CommonUser user) throws ProjectException {
        ProjectVersion currentVersion = getLastVersion();
        checkIn(user, currentVersion.getMajor(), currentVersion.getMinor());
    }

    public void checkIn(int major, int minor) throws ProjectException {
        checkIn(user, major, minor);
    }

    public void checkIn(CommonUser user, int major, int minor) throws ProjectException {
        save(user, major, minor);
        unlock(user);
        refresh();
    }

    public void checkOut() throws ProjectException {
        open();
        lock(user);
    }

    public void close() throws ProjectException {
        if (isCheckedOut()) {
            unlock(user);
        }
        refresh();
    }

    public void erase() throws ProjectException {
        getAPI().delete(user);
    }

    /** is checked-out by me? -- in LW + locked by me */
    public boolean isCheckedOut() {
        if (isLocalOnly()) {
            return false;
        }

        return isLockedByMe();
    }

    /** is deleted in DTR */
    public boolean isDeleted() {
        return getAPI().hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
    }

    public boolean isLocalOnly() {
        return false;
    }

    public boolean isLockedByMe() {
        if (isLocked()) {
            CommonUser lockedBy = getLockInfo().getLockedBy();
            // FIXME
            if (lockedBy.getUserName().equals(user.getUserName())) {
                return true;
            }
        }

        return false;
    }

    /** is opened by me? -- in LW */
    public boolean isOpened() {
        return true;
    }

    /** is opened other version? (not last) */
    public boolean isOpenedOtherVersion() {
        if (!isOpened()) {
            return false;
        }
        ProjectVersion max = getLastVersion();
        if (max == null) {
            return false;
        }
        return (!getVersion().equals(max));
    }

    public void open() throws ProjectException {
        openVersion(getLastVersion());
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        setAPI(getAPI().getVersion(version));
        refresh();
    }

    public void undelete() throws ProjectException {
        if (!isDeleted()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }

        try {
            removeProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
        } catch (PropertyException e) {
            throw new ProjectException("Failed to undelete project.", e);
        }
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        AProject project = (AProject) artefact;
        setDependencies(project.getDependencies());
        super.update(artefact, user, major, minor);
    }
    
    @Override
    public void smartUpdate(AProjectArtefact artefact, CommonUser user, int major, int minor) throws ProjectException {
        if (artefact.isModified()) {
            AProject project = (AProject) artefact;
            setDependencies(project.getDependencies());
            super.smartUpdate(artefact, user, major, minor);
        }
    }

    public boolean getCanCheckOut() {
        if (isLocalOnly() || isCheckedOut() || isLocked()) {
            return false;
        }

        return isGranted(PRIVILEGE_EDIT);
    }

    public boolean getCanClose() {
        return (!isLocalOnly() && isOpened());
    }

    public boolean getCanDelete() {
        if (isLocalOnly()) {
            // any user can delete own local project
            return true;
        }

        return (!isLocked() || isLockedByMe()) && isGranted(PRIVILEGE_DELETE);
    }

    public boolean getCanErase() {
        return (isDeleted() && isGranted(PRIVILEGE_ERASE));
    }

    public boolean getCanExport() {
        return getCanOpen();
    }

    public boolean getCanOpen() {
        if (isLocalOnly() || isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanCompare() {
        if (isLocalOnly()) {
            return false;
        }
        return isGranted(PRIVILEGE_READ);
    }

    public boolean getCanRedeploy() {
        if (isLocalOnly() || isCheckedOut()) {
            return false;
        }

        return isGranted(PRIVILEGE_DEPLOY);
    }

    public boolean getCanUndelete() {
        return (isDeleted() && isGranted(PRIVILEGE_EDIT));
    }
}
