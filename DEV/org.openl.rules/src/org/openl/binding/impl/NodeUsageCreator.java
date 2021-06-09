package org.openl.binding.impl;

import java.util.Optional;

import org.openl.binding.IBoundNode;

/**
 * Base interface of converters from {@link IBoundNode} to {@link NodeUsage}. Used in {@link NodeUsageFactory}
 *
 * @author Vladyslav Pikus
 */
interface NodeUsageCreator {

    /**
     * Verify if target bound node can be converted to {@link NodeUsage} type
     *
     * @param boundNode bound node to verify
     * @return true if given bound node is acceptable to convert
     */
    boolean accept(IBoundNode boundNode);

    /**
     * Create {@link NodeUsage} object from given bound node
     *
     * @param boundNode bound node to convert
     * @param sourceString source code
     * @param startIndex start index
     * @return converted node usage or empty
     */
    Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startIndex);

}
