package org.openl.cache;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public final class GenericKey {
    
    private Object[] objects;

    public GenericKey(Object... obj) {
        this.objects = obj;
    }

    @Override
    public boolean equals(Object x) {
        if(!(x instanceof GenericKey)){
            return false;
        }
        GenericKey anotherKey = (GenericKey)x;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        for (int i = 0; i < objects.length; i++) {
            equalsBuilder.append(objects[i], anotherKey.objects[i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        for (int i = 0; i < objects.length; i++) {
            hashCodeBuilder.append(objects[i]);
        }
        return hashCodeBuilder.toHashCode();
    }
}