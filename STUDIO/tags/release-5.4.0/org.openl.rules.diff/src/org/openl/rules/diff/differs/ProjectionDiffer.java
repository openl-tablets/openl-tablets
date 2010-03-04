package org.openl.rules.diff.differs;

import java.util.Set;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.tree.DiffProperty;

/**
 * Finds difference or compares 2 projections.
 * <p>
 * comparing projections must have the same type.
 * 
 * @author Aleh Bykhavets
 * 
 */
public interface ProjectionDiffer {
    boolean compare(Projection original, Projection other);
    Set<DiffProperty> getDiffProperties();
}
