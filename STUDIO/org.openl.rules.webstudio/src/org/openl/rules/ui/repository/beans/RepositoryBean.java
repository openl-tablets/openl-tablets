package org.openl.rules.ui.repository.beans;

import java.util.Date;
import java.util.List;

/**
 * Just displays meaningful repository name, nothing more.
 * 
 * @author Aleh Bykhavets
 *
 */
// TODO: Do we really need it?  Can we use null instead?
public class RepositoryBean implements Entity {

    private String name;
    
    /** {@inheritDoc} */
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    // ------ NOT SUPPORTED properties
    
    public void delete() {
        // not supported
    }

    public Date getCreated() {
        // not supported
        return null;
    }

    public List<AbstractEntityBean> getElements() {
        // not supported
        return null;
    }

    public Date getLastModified() {
        // not supported
        return null;
    }

    public String getLastModifiedBy() {
        // not supported
        return null;
    }

    public String getVersion() {
        // not supported
        return null;
    }

    public List<VersionBean> getVersionHistory() {
        // not supported
        return null;
    }
}
