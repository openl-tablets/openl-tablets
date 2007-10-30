package org.openl.rules.ui.repository.deploy.bean;

import org.openl.rules.ui.repository.deploy.DeploymentDescriptorEntry;

/**
 * Extention of <code>DeploymentDescriptorEntry</code> that provides <code>delete</code> method to JSF runtime. 
 */
public class EntryBean extends DeploymentDescriptorEntry {
    private DeploymentDescriptorBean parent;

    public EntryBean(String name, String version) {
        super(name, version);
    }

    public EntryBean(String name, String version, String messages) {
        super(name, version, messages);
    }

    public void delete() {
        if (parent != null) {
            parent.removeEntry(this);
        }
        parent = null;
    }

    void setParent(DeploymentDescriptorBean parent) {
        this.parent = parent;
    }
}
