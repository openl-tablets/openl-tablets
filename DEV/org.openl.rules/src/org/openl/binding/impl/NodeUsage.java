package org.openl.binding.impl;

/**
 * @author nsamatov.
 */
public interface NodeUsage {
    int getStart();

    int getEnd();

    String getDescription();

    String getUri();

    NodeType getNodeType();
}
