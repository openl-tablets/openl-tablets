package org.openl.rules.project.abstraction;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;

public class AProject extends AProjectFolder {

    public AProject(FolderAPI api) {
        super(api, null);
    }

    @Override
    public AProject getProject() {
        return this;
    }

    @Override
    public void delete() throws ProjectException {
        throw new ProjectException("Unsupported operation.");
    }

    public void delete(CommonUser user) throws ProjectException {
        if (isLocked() && !isLockedByUser(user)) {
            throw new ProjectException("Cannot delete project ''{0}'' while it is locked by other user!", null,
                    getName());
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

    public void save(CommonUser user) throws ProjectException {
        commit(user);
        unlock(user);
        refresh();
    }

    public void edit(CommonUser user) throws ProjectException {
        lock(user);
    }

    public void close(CommonUser user) throws ProjectException {
        if (isLockedByUser(user)) {
            unlock(user);
        }
        refresh();
    }

    public void erase(CommonUser user) throws ProjectException {
        getAPI().delete(user);
    }

    public boolean isDeleted() {
        return getAPI().hasProperty(ArtefactProperties.PROP_PRJ_MARKED_4_DELETION);
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

    public AProject getProjectVersion(CommonVersion version) throws ProjectException {
        return new AProject(getAPI().getVersion(version));
    }

    public boolean getOpenedForEditing() {
        return false;
    }
}
