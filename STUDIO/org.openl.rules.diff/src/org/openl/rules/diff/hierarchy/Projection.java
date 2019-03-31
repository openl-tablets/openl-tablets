package org.openl.rules.diff.hierarchy;

import java.util.Collection;
import java.util.List;

/**
 * Projection of an artifact.
 * <p>
 * The Projection can be considered as one of many Views of any object.
 * <p>
 * A Projection shows some properties of actual artifact and can hides others, that are not relevant to domain of the
 * Projection.
 * <p>
 * Also Projection can have own hierarchy of sub elements which can be completely different from hierarchy of physical
 * artifact.
 * <p>
 * Child of the Projection is Projection that allows multiple views on each element depending on its position in
 * hierarchy. But that can lead to extreme memory allocation also. This feature should be used with care.
 * 
 * @author Aleh Bykhavets
 * 
 */
public interface Projection {
    /**
     * Logical name of data.
     * <p>
     * Different projections can have different names and the name of a projection can differ from name of an artifact.
     * <p>
     * The logical name of a projection should reflect its meaning in specific domain.
     * 
     * @return logical name
     */
    String getName();

    String getType();

    /**
     * Projection specific properties of the artifact.
     * <p>
     * An artifact can have a lot of properties and not all of them can exist in the projection. It is possible that a
     * projection will have no properties even if the artifact has some.
     * <p>
     * If there is no properties empty (zero length) array is returned.
     * 
     * @return projection specific properties
     */
    Collection<ProjectionProperty> getProperties();

    /**
     * Get property of the artifact by name.
     * 
     * @param propertyName name of property to be found
     * @return property or null
     */
    ProjectionProperty getProperty(String propertyName);

    /**
     * Get raw value of property. If there is no such property returns null.
     * <p>
     * Note that null can mean that such property exists but its value is null.
     * 
     * @param propertyName name of property
     * @return raw value of property or null
     */
    Object getPropertyValue(String propertyName);

    /**
     * Direct children of the projection.
     * <p>
     * If there is no children empty (zero length) array is returned.
     *
     * @return direct children or empty array
     */
    List<Projection> getChildren();
}
