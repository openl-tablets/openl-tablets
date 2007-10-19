package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.ui.repository.beans.CannotFindEntityException;
import org.openl.rules.ui.repository.beans.VersionBean;
import org.openl.rules.repository.*;
import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;
import java.util.LinkedList;

/**
 * Abstract UI Bean Handler.
 * Handler is one who really does work.
 * 
 * @author Aleh Bykhavets
 *
 */
public abstract class BeanHandler {
    protected Context context;

    public BeanHandler(Context context) {
        this.context = context;
    }

    /**
     * Initializes UI Bean. The handler sets all data properties of the UI Bean
     * from the repository entity.
     * Also sets system properties: ID and Handler. 
     * 
     * @param bean UI bean to be initialized
     * @param entity a repository entity
     */
    protected void initBean(AbstractEntityBean bean, REntity entity) {
        // set system properties
        bean.setId(generateId(entity));
        bean.setHandler(this);

        // set data properties
        bean.setName(entity.getName());
        RVersion v = entity.getBaseVersion();
        bean.setVersion(v.getName());
        bean.setLastModified(v.getCreated());
        bean.setLastModifiedBy(v.getCreatedBy().getName());
    }
    
    /**
     * Deletes a repository entity
     * 
     * @param bean UI bean for a repository entity
     */
    public void delete(AbstractEntityBean bean) {
        try {
            REntity entity = getEntityById(bean.getId());
            entity.delete();
        } catch (RDeleteException e) {
            // TODO add 2 log
            context.getMessageQueue().addMessage(e);
        } catch (CannotFindEntityException e) {
            // TODO add 2 log
            context.getMessageQueue().addMessage(e);
        } finally {
            context.refresh();
        }
    }

    /**
     * Gets list of versions for a repository entity
     * 
     * @param bean UI bean for a repository entity
     * @return list of versions (always not null).
     */
    public List<VersionBean> getVersions(AbstractEntityBean bean) {
        LinkedList<VersionBean> result = new LinkedList<VersionBean>();

        try {
            REntity entity = getEntityById(bean.getId());

            for (RVersion version : entity.getVersionHistory()) {
                VersionBean vb = new VersionBean();

                vb.setName(version.getName());
                vb.setCreated(version.getCreated());
                vb.setCreatedBy(version.getCreatedBy().getName());

                // newest at top -- 1.4, 1.3, 1.2 ...
                result.addFirst(vb);
            }
        } catch (RRepositoryException e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        } catch (CannotFindEntityException e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }

        return result;
    }

    // ------ protected ------
    
    /**
     * Generates id for a repository entity.
     * Currently id is path of the entity. 
     * 
     * @param entity a repository entity
     * @return id of the entity or <code>null</code> if cannot determine one.
     */
    protected String generateId(REntity entity) {
        String id;
        try {
            id = entity.getPath();
        } catch (RRepositoryException e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
            id = null;
        }

        return id;
    }

    /**
     * Reverse operation.
     * Finds a repository entity by its id.
     *  
     * @param id id of a entity
     * @return a repository entity or <code>null</code> if cannot find
     */
    protected REntity getEntityById(String id) throws CannotFindEntityException {
        REntity result = null;

        LinkedList<String> names = splitId(id);
        if (!names.isEmpty()) {
            String name = names.removeFirst();

            try {
                for (RProject project : context.getRepository().getProjects()) {
                    if (project.getName().equals(name)) {
                        // found
                        if (names.isEmpty()) {
                            result = project;
                        } else {
                            result = findEntityDeeper(project.getRootFolder(), names);
                        }
                        break;
                    }
                }
            } catch (RRepositoryException e) {
                throw new CannotFindEntityException("Failed to find entity due repository exception", e);
            }
        }

        if (result == null) {
            throw new CannotFindEntityException("Cannot Find entity by ID");
        }
        
        return result;
    }

    // ------ private ------
    
    /**
     * Splits id on elements.
     * I.e. breaks path on entity names.
     * 
     * @param id id (path)
     * @return splitted path as a list
     */
    private LinkedList<String> splitId(String id) {
        LinkedList<String> names = new LinkedList<String>();
        String[] parts = id.split("/");

        for (String part : parts) {
            // remove first empty string
            if (part.length() == 0) continue;
            
            names.add(part);
        }

        return names;
    }

    /**
     * Finds a repository entity by path.
     * Uses recursive method.
     * 
     * @param folder current repository folder
     * @param names splitted path
     * @return found entity or <code>null</code>
     * @throws RRepositoryException if failed
     */
    private REntity findEntityDeeper(RFolder folder, LinkedList<String> names) throws RRepositoryException {
        String name = names.removeFirst();

        // check sub folders
        for (RFolder f : folder.getFolders()) {
            if (f.getName().equals(name)) {
                // found
                if (names.isEmpty()) {
                    return f;
                }
                // deeper and deeper
                return findEntityDeeper(f, names);
            }
        }

        if (!names.isEmpty()) {
            // failed to find sub folder(s)
            return null;
        }

        // check files
        for (RFile f : folder.getFiles()) {
            if (f.getName().equals(name)) {
                // found
                return f;
            }
        }

        // no file with given name
        return null;
    }
}
