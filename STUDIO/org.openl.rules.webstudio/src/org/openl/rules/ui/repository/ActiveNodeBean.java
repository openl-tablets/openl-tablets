package org.openl.rules.ui.repository;

import java.util.Date;
import java.util.List;

import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.ui.repository.beans.Entity;
import org.openl.rules.ui.repository.beans.VersionBean;

public class ActiveNodeBean implements Entity {
    private Entity bean;
    private String type;
    
    public void setBean(Entity bean) {
        System.out.println("ANB-bean=" + bean);
        this.bean = bean;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }

    public void delete() {
        System.out.println("ANB-delete...");
        bean.delete();
    }
    
    public Date getCreated() {
        return bean.getCreated();
    }

    public List<AbstractEntityBean> getElements() {
        return bean.getElements();
    }

    public Date getLastModified() {
        return bean.getLastModified();
    }

    public String getLastModifiedBy() {
        return bean.getLastModifiedBy();
    }

    public String getName() {
        return bean.getName();
    }

    public String getVersion() {
        return bean.getVersion();
    }

    public List<VersionBean> getVersionHistory() {
        return bean.getVersionHistory();
    }
}
