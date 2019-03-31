package org.openl.rules.diff.differs;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;

public class ProjectionDifferImpl implements ProjectionDiffer {
    // @Override
    public boolean compare(Projection original, Projection other) {
        Map<String, ProjectionProperty> map1 = buildMap(original);
        Map<String, ProjectionProperty> map2 = buildMap(other);

        MergeResult merged = MergeResult.mergeNames(map1.keySet(), map2.keySet());

        if (merged.getAdded().length > 0 || merged.getRemoved().length > 0) {
            // Different properties
            return false;
        }

        for (String propertyName : merged.getCommon()) {
            ProjectionProperty p1 = map1.get(propertyName);
            ProjectionProperty p2 = map2.get(propertyName);

            if (!isEquals(p1, p2)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isEquals(ProjectionProperty p1, ProjectionProperty p2) {
        Object v1 = p1.getRawValue();
        Object v2 = p2.getRawValue();

        if (v1 == null) {
            return v2 == null;
        } else {
            return v1.equals(v2);
        }
    }

    protected static Map<String, ProjectionProperty> buildMap(Projection projection) {
        Map<String, ProjectionProperty> map = new HashMap<>();

        for (ProjectionProperty property : projection.getProperties()) {
            map.put(property.getName(), property);
        }
        return map;
    }
}
