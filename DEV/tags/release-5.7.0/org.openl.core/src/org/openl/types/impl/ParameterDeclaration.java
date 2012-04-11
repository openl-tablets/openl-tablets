/*
 * Created on Nov 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

/**
 * @author snshor
 *
 */
public class ParameterDeclaration implements IParameterDeclaration {
    private IOpenClass type;
    private String name;
    private int direction;

    public ParameterDeclaration(IOpenClass type, String name) {
        this(type, name, IN);
    }

    public ParameterDeclaration(IOpenClass type, String name, int direction) {
        this.type = type;
        this.name = name;
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
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

        return new EqualsBuilder().append(name, paramDecl.name).append(type, paramDecl.getType())
            .append(direction, paramDecl.getDirection()).isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder().append(name).append(type)
            .append(direction).toHashCode();
        return hashCode;
    }

}
