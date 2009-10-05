package org.openl.rules.diff.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.rules.diff.differs.ProjectionDiffer;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.hierarchy.Projection;

import org.openl.rules.diff.tree.DiffProperty;
import org.openl.rules.diff.tree.DiffStatus;

import static org.openl.rules.diff.tree.DiffStatus.*;

public class ProjectionDifferImpl implements ProjectionDiffer {

    private Set<DiffProperty> diffProperties = null;
    
//    @Override
    public boolean compare(Projection original, Projection other) {
        diffProperties = new HashSet<DiffProperty>();
        Map<String, ProjectionProperty> map1, map2;
        map1 = buildMap(original);
        map2 = buildMap(other);

        MergeResult merged = MergeResult.mergeNames(map1.keySet(), map2.keySet());

        for (String propertyName : merged.getAdded()) {
            ProjectionProperty p = map2.get(propertyName);
            diffProperties.add(new DiffPropertyImpl(p, ADDED));
        }
        
        for (String propertyName : merged.getRemoved()) {
            ProjectionProperty p = map2.get(propertyName);
            diffProperties.add(new DiffPropertyImpl(p, REMOVED));
        }
        
        for (String propertyName : merged.getCommon()) {
            ProjectionProperty p1 = map1.get(propertyName);
            ProjectionProperty p2 = map2.get(propertyName);

            if (!isEquals(p1, p2)) {
                diffProperties.add(new DiffPropertyImpl(p2, DIFFERS));
            } else {
                diffProperties.add(new DiffPropertyImpl(p2, EQUALS));
            }
        }

        for (DiffProperty property : diffProperties) {
            DiffStatus status = property.getDiffStatus();
            if (!status.equals(EQUALS)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isEquals(ProjectionProperty p1, ProjectionProperty p2) {
        Class type1 = p1.getType();
        Class type2 = p2.getType();

        if (type1 != type2) {
            return false;
        }

        Object v1 = p1.getRawValue();
        Object v2 = p2.getRawValue();

        if (v1 == null) {
            return (v1 == v2);
        } else {
            return v1.equals(v2);
        }
    }

    static Map<String, ProjectionProperty> buildMap(Projection projection) {
        Map<String, ProjectionProperty> map = new HashMap<String, ProjectionProperty>();

        ProjectionProperty[] properties = projection.getProperties();
        for (ProjectionProperty property : properties) {
            if (property.isComparable()) {
                map.put(property.getName(), property);
            }
        }
        return map;
    }

    public Set<DiffProperty> getDiffProperties() {
        return diffProperties;
    }

}
