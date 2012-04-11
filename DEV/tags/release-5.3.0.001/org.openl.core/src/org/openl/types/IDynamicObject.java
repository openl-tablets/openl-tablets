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

    Map<String, Object> getFieldValues();

    IOpenClass getType();

    void setFieldValue(String name, Object value);

}
