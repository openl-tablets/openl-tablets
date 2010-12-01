package org.openl.rules.diff.xls;

import org.openl.rules.diff.differs.ProjectionDifferImpl;
import org.openl.rules.diff.hierarchy.Projection;

public class XlsProjectionDiffer extends ProjectionDifferImpl {
//  @Override
    public boolean compare(Projection original, Projection other) {
        boolean selfEqual = true;
        // TODO compare XLS tables here
        // Sometimes there is no reason to create Big Tree with Projections
        // Just take and compare it here
        // Then store result (Table and Filter) and stop "suffering" )
        return (selfEqual && super.compare(original, other));
    }
}
