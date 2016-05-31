/*
 * Created on Jun 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenClassHolder;
import org.openl.types.IOpenFactory;
import org.openl.types.IOpenSchema;

/**
 * @author snshor
 *
 */
public abstract class AOpenSchema implements IOpenSchema {
    protected IOpenFactory factory;

    protected Map<String, IOpenClassHolder> allClasses;

    protected AOpenSchema(IOpenFactory factory) {
        this.factory = factory;
    }

    protected synchronized Map<String, IOpenClassHolder> allClasses() {
        if (allClasses == null) {
            allClasses = buildAllClasses();
        }
        return allClasses;
    }

    protected abstract Map<String, IOpenClassHolder> buildAllClasses();

    public IOpenFactory getFactory() {
        return factory;
    }

    public synchronized IOpenClass getType(String name) {
        IOpenClassHolder holder = allClasses().get(name);

        return holder == null ? null : holder.getOpenClass();
    }
}
