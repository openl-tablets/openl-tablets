/**
 * 
 */
package org.openl.types.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * 
 * Key for IOpenMethod.
 *
 */
public final class MethodKey {
    private String name;
    private IOpenClass[] pars;

    public MethodKey(IOpenMethod om) {
        name = om.getName();
        pars = om.getSignature().getParameterTypes();
    }

    public MethodKey(String name, IOpenClass[] pars) {
        this.name = name;
        this.pars = pars;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodKey)) {
            return false;
        }
        MethodKey mk = (MethodKey) obj;

        return new EqualsBuilder().append(name, mk.name).append(pars, mk.pars).isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder().append(name).append(pars).toHashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        boolean first = true;
        for (IOpenClass c : pars) {
            if (!first) {
                sb.append(",");
            }
            sb.append(c.getName());
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

}