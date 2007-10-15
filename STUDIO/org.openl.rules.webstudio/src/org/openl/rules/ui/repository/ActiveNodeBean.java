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
    private Entity bean;
    private String type;
    
    public void setBean(Entity bean) {
        this.bean = bean;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }

    // ------ entity properties ------
    
    /** {@inheritDoc} */
    public void delete() {
        bean.delete();
    }
    
    /** {@inheritDoc} */
    public Date getCreated() {
        return bean.getCreated();
    }

    /** {@inheritDoc} */
    public List<AbstractEntityBean> getElements() {
        return bean.getElements();
    }

    /** {@inheritDoc} */
    public Date getLastModified() {
        return bean.getLastModified();
    }

    /** {@inheritDoc} */
    public String getLastModifiedBy() {
        return bean.getLastModifiedBy();
    }

    /** {@inheritDoc} */
    public String getName() {
        return bean.getName();
    }

    /** {@inheritDoc} */
    public String getVersion() {
        return bean.getVersion();
    }

    /** {@inheritDoc} */
    public List<VersionBean> getVersionHistory() {
        return bean.getVersionHistory();
    }
}
