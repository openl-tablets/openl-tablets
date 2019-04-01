package org.openl.rules.diff.differs;

import org.openl.rules.diff.hierarchy.Projection;

/**
 * Finds difference or compares 2 projections.
 * <p>
 * comparing projections must have the same type.
 *
 * @author Aleh Bykhavets
 *
 */
public interface ProjectionDiffer {
    /**
     * Compare two projections from different sources. Both is assumed to be of the same type and not null.
     *
     * @param original original projection
     * @param other comparing projection
     * @return true if they are selfEqual
     */
    boolean compare(Projection original, Projection other);
}
