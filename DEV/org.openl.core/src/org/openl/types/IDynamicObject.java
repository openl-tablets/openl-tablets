/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import java.util.Map;

/**
 * @author snshor
 *
 */
public interface IDynamicObject {

    Object getFieldValue(String name);

    void setFieldValue(String name, Object value);

    /**
     * Gets map of all field values with names as map keys. The changes in map itself will not affect the instance.
     * However, changes in values object will affect field values.
     *
     * @return Map of field names as keys and field values as values.
     */
    Map<String, Object> getFieldValues();

    IOpenClass getType();
}
