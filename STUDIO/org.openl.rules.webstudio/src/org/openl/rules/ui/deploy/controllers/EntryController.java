package org.openl.rules.ui.deploy.controllers;

import org.openl.rules.ui.deploy.DeploymentDescriptorEntry;

/**
 * Extention of <code>DeploymentDescriptorEntry</code> that provides <code>delete</code> method to JSF runtime. 
 */
public class EntryController extends DeploymentDescriptorEntry {
    private DeploymentDescriptorController parent;

    public EntryController(String name, String version) {
        super(name, version);
    }

    public EntryController(String name, String version, String messages) {
        super(name, version, messages);
    }

    public void delete() {
        if (parent != null) {
            parent.removeEntry(this);
        }
        parent = null;
    }

    void setParent(DeploymentDescriptorController parent) {
        this.parent = parent;
    }
}
