package org.openl.rules.ui.repository;

import java.util.Date;
import java.util.List;

import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.ui.repository.beans.Entity;
import org.openl.rules.ui.repository.beans.VersionBean;

/**
 * Short for {@link UserSessionBean#getSelected()}.getDataBean().
 * <p>
 * In faces activeNode.* map on
 * <ul>
 * <li>userSession.selected.dataBean.*</li>
 * <li>userSession.selected.type</li>
 * </ul>
 * 
 * @author Aleh Bykhavets
 *
 */
public class ActiveNodeBean implements Entity {
    private UserSessionBean userSession;
    
    public void setUserSession(UserSessionBean userSession) {
        this.userSession = userSession;
    }
    
    public String getType() {
        return userSession.getSelected().getType();
    }

    private Entity getBean() {
        Entity entity = (Entity) userSession.getSelected().getDataBean();
        return entity;
    }
    
    // ------ entity properties ------
    
    /** {@inheritDoc} */
    public void delete() {
        getBean().delete();
    }
    
    /** {@inheritDoc} */
    public Date getCreated() {
        return getBean().getCreated();
    }

    /** {@inheritDoc} */
    public List<? extends Entity> getElements() {
        return getBean().getElements();
    }

    /** {@inheritDoc} */
    public Date getLastModified() {
        return getBean().getLastModified();
    }

    /** {@inheritDoc} */
    public String getLastModifiedBy() {
        return getBean().getLastModifiedBy();
    }

    /** {@inheritDoc} */
    public String getName() {
        return getBean().getName();
    }

    /** {@inheritDoc} */
    public String getVersion() {
        return getBean().getVersion();
    }

    /** {@inheritDoc} */
    public List<VersionBean> getVersionHistory() {
        return getBean().getVersionHistory();
    }
}
