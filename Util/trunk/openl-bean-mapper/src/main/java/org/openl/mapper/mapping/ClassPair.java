package org.openl.mapper.mapping;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

class ClassPair {

    private Class<?> classA;
    private Class<?> classB;

    public ClassPair(Class<?> classA, Class<?> classB) {
        this.classA = classA;
        this.classB = classB;
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder()
            .append(classA)
            .append(classB)
            .toHashCode();
        
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        
        if (!(obj instanceof ClassPair)) {
            return false;
        }
        
        ClassPair p = (ClassPair) obj;

        return new EqualsBuilder()
            .append(classA, p.classA)
            .append(classB, p.classB)
            .isEquals();
    }
}
