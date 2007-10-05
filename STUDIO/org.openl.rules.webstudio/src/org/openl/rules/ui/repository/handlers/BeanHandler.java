package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.ui.repository.beans.VersionBean;
import org.openl.rules.repository.*;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;
import java.util.LinkedList;

public class BeanHandler {
    protected Context context;

    public BeanHandler(Context context) {
        this.context = context;
    }

    public void updateName(AbstractEntityBean bean) {
        REntity entity = getEntityById(bean.getId());
        
        System.out.println("* updateName: " + bean.getId());
//        entity.setName(bean.getName());
    }

    public void delete(AbstractEntityBean bean) {
        REntity entity = getEntityById(bean.getId());
        
        System.out.println("* delete: " + bean.getId());
//        entity.delete();
    }

    protected void initBean(AbstractEntityBean bean, REntity entity) {
        bean.setId(generateId(entity));
        bean.setHandler(this);

        bean.setName(entity.getName());
        RVersion v = entity.getBaseVersion();
        bean.setVersion(v.getName());
        bean.setLastModified(v.getCreated());
        bean.setLastModifiedBy(v.getCreatedBy().getName());
    }
    
    public List<VersionBean> getVersions(AbstractEntityBean bean) {
        List<VersionBean> result = new LinkedList<VersionBean>();

        REntity entity = getEntityById(bean.getId());

        try {
            for (RVersion version : entity.getVersionHistory()) {
                VersionBean vb = new VersionBean();

                vb.setName(version.getName());
                vb.setCreated(version.getCreated());
                vb.setCreatedBy(version.getCreatedBy().getName());

                result.add(vb);
            }
        } catch (RRepositoryException e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }

        return result;
    }

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

    protected REntity getEntityById(String id) {
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
                // TODO: log exception
                context.getMessageQueue().addMessage(e);
            }
        }

        return result;
    }

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
