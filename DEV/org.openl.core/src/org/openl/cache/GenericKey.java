package org.openl.cache;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Arrays;

/**
 * This is immutable object for using it as a key. Build key by array of
 * objects. This is instance controlled class. For instantiate class use static
 * factory method "getInstance" [EJ1].
 * 
 */
public final class GenericKey {
    private static final GenericKey EMPTY_GENERIC_KEY = new GenericKey(new Object[] {});
    private Object[] objects;

    private GenericKey(Object... objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects arg can't be null");
        }
        this.objects = Arrays.copyOf(objects, objects.length);
    }

    /**
     * Returns a key instance for this objects
     * 
     * @param objects
     * @return
     */
    public static GenericKey getInstance(Object... objects) {
        if (objects == null || objects.length == 0) {
            return EMPTY_GENERIC_KEY;
        }
        return new GenericKey(objects);
    }

    @Override
    public boolean equals(Object x) {
        if (!(x instanceof GenericKey)) {
            return false;
        }
        GenericKey anotherKey = (GenericKey) x;
        if (objects.length != anotherKey.objects.length) {
            return false;
        }
        // FIXME: Remove usage of the EqualsBuilder
        //
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        for (int i = 0; i < objects.length; i++) {
            equalsBuilder.append(objects[i], anotherKey.objects[i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        return objects != null ? Arrays.hashCode(objects) : 0;
    }
}