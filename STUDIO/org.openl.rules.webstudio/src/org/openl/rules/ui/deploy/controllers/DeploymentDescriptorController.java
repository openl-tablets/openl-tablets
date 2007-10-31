package org.openl.rules.ui.deploy.controllers;

import static org.openl.rules.ui.repository.UiConst.OUTCOME_SUCCESS;
import org.openl.rules.ui.deploy.DeploymentDescriptor;
import org.openl.rules.ui.deploy.DeploymentDescriptorEntry;

import java.util.List;
import java.util.ArrayList;

public class DeploymentDescriptorController implements DeploymentDescriptor {
    private List<DeploymentDescriptorEntry> entries;
    {
        entries = new ArrayList<DeploymentDescriptorEntry>();
        entries.add(new EntryController("Project 1", "1.2.1"));
        entries.add(new EntryController("Project 2", "1.2.2", "Conflicts with project 5 v1.0.4"));
        entries.add(new EntryController("Project 5", "1.0.4"));
        for (DeploymentDescriptorEntry eb : entries) {
            ((EntryController) eb).setParent(this);
        }
    }

    public String deploy() {
        return OUTCOME_SUCCESS; 
    }

    public String checkIn() {
        return OUTCOME_SUCCESS;
    }

    public String checkOut() {
        return OUTCOME_SUCCESS;
    }

    public synchronized List<DeploymentDescriptorEntry> getEntries() {
        return entries;
    }

    public boolean isCheckinable() {
        return true;
    }

    public boolean isCheckoutable() {
        return true;
    }

    /**
     * Removes an entry from this deployment descriptor.
     *
     * @param entry an entry to remove.
     */
    public synchronized boolean removeEntry(DeploymentDescriptorEntry entry) {
        return entries != null && entries.remove(entry);
    }

    /**
     * Adds new entry to this deployment descriptor. If the entry is already present in this descriptor the methods
     * does not add it again.
     *
     * @param entry an entry to add
     * @return if entry was added.
     * @throws NullPointerException if <code>entry</code> is <code>null</code>
     */
    public boolean addEntry(DeploymentDescriptorEntry entry) {
        if (entry == null) {
            throw new NullPointerException("entry is null");
        }

        if (! (entry instanceof EntryController)) {
            entry = new EntryController(entry.getName(), entry.getVersion(), entry.getMessages());
        }
        ((EntryController) entry).setParent(this);

        return !containsEntry(entry) && entries.add(entry);
    }

    public boolean containsEntry(DeploymentDescriptorEntry entry) {
        return entries.contains(entry);
    }


}
