/*
 * Created on Jun 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;

import org.openl.types.IOpenFactory;
import org.openl.types.IOpenSchema;

/**
 * @author snshor
 *
 */
public abstract class AOpenFactory implements IOpenFactory {

    HashMap<String, IOpenSchema> schemaPool = new HashMap<String, IOpenSchema>();

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenFactory#getSchema(java.lang.String, boolean)
     */
    public synchronized IOpenSchema getSchema(String uri, boolean reload) throws Exception {
        IOpenSchema schema = null;
        if (!reload) {
            schema = schemaPool.get(uri);
            if (schema != null) {
                return schema;
            }
        }

        schema = loadSchema(uri);

        if (schema == null) {
            schemaPool.remove(uri);
        } else {
            schemaPool.put(uri, schema);
        }

        return schema;
    }

    public abstract IOpenSchema loadSchema(String uri) throws Exception;

}
