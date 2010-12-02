package org.openl.rules.project.abstraction;

import java.util.Collection;

import org.openl.rules.project.impl.ProjectArtefactAPI;
import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.ValueType;
import org.openl.rules.workspace.props.impl.PropertyImpl;

import static org.openl.rules.security.Privileges.*;
import static org.openl.rules.security.SecurityUtils.check;
import static org.openl.rules.security.SecurityUtils.isGranted;

public class AProject extends AProjectFolder{
    protected CommonUser user;

    public AProject(ProjectArtefactAPI api, CommonUser user) {
        super(api, null);
        this.user = user;
    }

    @Override
    public AProject getProject() {
        return this;
    }

    public Collection<ProjectDependency> getDependencies() {
        return impl.getDependencies();
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) throws ProjectException {
        impl.setDependencies(dependencies);
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
            addProperty(new PropertyImpl(JcrNT.PROP_PRJ_MARKED_4_DELETION, ValueType.BOOLEAN, true));
        } catch (PropertyException e) {
            throw new ProjectException("Failed to mark project as deleted.", e);
        }
    }

    public void checkIn() throws ProjectException {
        ProjectVersion currentVersion = getLastVersion();
        checkIn(currentVersion.getMajor(), currentVersion.getMinor());
    }

    public void checkIn(int major, int minor) throws ProjectException {
        impl.commit(user, major, minor);
        close();
    }

    public void checkOut() throws ProjectException {
        open();
        impl.lock(user);
    }

    public void close() throws ProjectException {
        if(isCheckedOut()){
            impl.unlock(user);
        }
        impl.close(user);
        refresh();
    }

    public void erase() throws ProjectException {
        impl.delete(user);
    }

    public LockInfo getLockInfo() {
        return impl.getLockInfo();
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
        return impl.hasProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION);
    }

    /** no such project in DTR */
    public boolean isLocalOnly() {
        return impl.isLocalOnly();
    }

    /** is locked in DTR */
    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByMe() {
        if (isLocked()) {
            WorkspaceUser lockedBy = getLockInfo().getLockedBy();
            if (lockedBy.equals(user)) {
                return true;
            }
        }

        return false;
    }

    /** is opened by me? -- in LW */
    public boolean isOpened() {
        return impl.isOpened();
    }

    /** is opened other version? (not last) */
    public boolean isOpenedOtherVersion() {
        if (!isOpened()) {
            return false;
        }
        ProjectVersion max = getLastVersion();
        if(max == null){
            return false;
        }
        return (!getVersion().equals(max));
    }

    public void open() throws ProjectException {
        openVersion(getLastVersion());
    }

    public void openVersion(CommonVersion version) throws ProjectException {
        impl.openVersion(version);
        refresh();
    }

    public void undelete() throws ProjectException{
        if (!isDeleted()) {
            throw new ProjectException("Cannot undelete non-marked project ''{0}''!", null, getName());
        }
        
        try {
            removeProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION);
        } catch (PropertyException e) {
            throw new ProjectException("Failed to undelete project.", e);
        }
    }

    @Override
    public void update(AProjectArtefact artefact) throws ProjectException {
        super.update(artefact);
        AProject project = (AProject) artefact;
        setDependencies(project.getDependencies());
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
