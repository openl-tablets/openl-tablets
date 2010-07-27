package org.openl.runtime;

import org.openl.conf.IUserContext;
import org.openl.source.IOpenSourceCodeModule;

/**
 * 
 * EngineFactoryDefinition handles context of users environment{@link IUserContext} and source code of rule {@link IOpenSourceCodeModule}.
 *
 */
public class EngineFactoryDefinition {

    IUserContext ucxt;

    IOpenSourceCodeModule sourceCode;

    public EngineFactoryDefinition(IUserContext ucxt, IOpenSourceCodeModule sourceCode) {
        this.ucxt = ucxt;
        this.sourceCode = sourceCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof EngineFactoryDefinition) {
            EngineFactoryDefinition fd = (EngineFactoryDefinition) obj;

            return ucxt == fd.ucxt && sourceCode.getUri(0).equals(fd.sourceCode.getUri(0));

        }

        return false;
    }

    @Override
    public int hashCode() {
        return ucxt.hashCode() * 37 + sourceCode.getUri(0).hashCode();
    }

}
