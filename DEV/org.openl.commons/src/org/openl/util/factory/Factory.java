package org.openl.util.factory;

/**
 * An abstraction of a factory. This abstraction allows to create an object
 * instance according to an input parameter.
 * 
 * @author Yury Molchan
 */
public interface Factory<K, V> {

    /**
     * Creates an instance of something using an input parameter.
     * 
     * @param param the input parameter which can be used for creating the
     *            object instance.
     * @return the created object instance
     */
    V create(K param);
}
