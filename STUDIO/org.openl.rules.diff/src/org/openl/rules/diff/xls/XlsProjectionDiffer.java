package org.openl.rules.diff.xls;

import org.openl.rules.diff.differs.ProjectionDifferImpl;
import org.openl.rules.diff.hierarchy.Projection;

public class XlsProjectionDiffer extends ProjectionDifferImpl {
//  @Override
    public boolean compare(Projection original, Projection other) {
        // TODO compare XLS tables here
        // Sometimes there is no reason to create Big Tree with Projections
        // Just take and compare it here
        // Then store result (Table and Filter) and stop "suffering" )
        XlsProjection p1 = (XlsProjection) original;
        XlsProjection p2 = (XlsProjection) other;

        boolean selfEqual = true;
        if (p2 != null && p2.getDiffCells() != null) {
            // have different cells -- cannot be equal
            selfEqual = false;
        }

        return (super.compare(original, other) && selfEqual);
    }
}
