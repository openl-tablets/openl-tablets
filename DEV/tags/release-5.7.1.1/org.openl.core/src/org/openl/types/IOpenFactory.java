/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 *
 * OpenFactory is responsible for the actual creation of the OpenClasses and
 * OpenSchemas
 *
 */

public interface IOpenFactory {

    /**
     * Produces a schema according to uri
     *
     * @param uri
     * @param reload if true forces reloading of the schema
     * @return
     */

    IOpenSchema getSchema(String uri, boolean reload) throws Exception;

}
