package org.openl.rules.ui.repository.deploy;

/**
 * Abstraction of deployment descriptor.
 */
public interface DeploymentDescriptor {
    /**
     * Removes an entry from this deployment descriptor.
     * 
     * @param entry an entry to remove.
     * @return if <code>entry</code> was really removed, that is it was present in the descriptor before method call
     */
    boolean removeEntry(DeploymentDescriptorEntry entry);

    /**
     * Adds new entry to this deployment descriptor. If the entry is already present in this descriptor the methods
     * does not add it again.
     *
     * @param entry an entry to add
     * @return if entry was added.
     * @throws NullPointerException if <code>entry</code> is <code>null</code>
     */
    boolean addEntry(DeploymentDescriptorEntry entry);

    boolean containsEntry(DeploymentDescriptorEntry entry);
}
