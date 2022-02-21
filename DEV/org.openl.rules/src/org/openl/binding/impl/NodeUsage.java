package org.openl.binding.impl;

/**
 * @author nsamatov.
 */
public interface NodeUsage {
    int getStart();

    /**
     * @return the ending index, exclusive
     */
    int getEnd();

    String getDescription();

    String getUri();

    NodeType getNodeType();
}
