/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class FormalParameter implements IFormalParameter {

    String name;
    IOpenClass type;

    public FormalParameter(String name, IOpenClass type) {
        this.name = name;
        this.type = type;
    }

    /**
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    @Override
    public IOpenClass getType() {
        return type;
    }

}
