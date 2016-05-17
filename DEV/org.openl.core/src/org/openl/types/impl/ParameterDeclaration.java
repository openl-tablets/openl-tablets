/*
 * Created on Nov 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.util.ClassUtils;

/**
 * @author snshor
 *
 */
public class ParameterDeclaration implements IParameterDeclaration {

    private IOpenClass type;
    private String name;

    public ParameterDeclaration(IOpenClass type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getDisplayName(int mode) {
        return name;
    }

    public String getName() {
        return name;
    }

    public IOpenClass getType() {
        return type;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParameterDeclaration)) {
            return false;
        }
        ParameterDeclaration paramDecl = (ParameterDeclaration) obj;

        return new EqualsBuilder().append(name, paramDecl.name).append(type, paramDecl.getType()).isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder().append(name).append(type).toHashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        return ClassUtils.getShortClassName(type.getInstanceClass()) + " " +name;
    }

}
