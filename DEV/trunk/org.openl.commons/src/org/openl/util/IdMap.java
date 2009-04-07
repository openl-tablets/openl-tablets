/*
 * Created on Feb 14, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.util;

/**
 * @author snshor
 *
 */
public class IdMap {

    public interface IdAdaptor {
        int getId(Object obj);
    }
    static public class IdObjectAdaptor implements IdAdaptor {

        public int getId(Object obj) {
            return ((IdObject) obj).getId();
        }

    }

    static final public IdObjectAdaptor ID_OBJECT_ADAPTOR = new IdObjectAdaptor();

    IdAdaptor adaptor;

    double freeSpaceRatio;
    int capacityGuard;

    double growthFactor;

    //
    int size = 0;

    Object[] data;

    public IdMap(int initialCapacity) {
        this(initialCapacity, ID_OBJECT_ADAPTOR, 1.2, 2.0);
    }

    public IdMap(int initialCapacity, IdAdaptor adaptor, double freeSpaceRatio, double growthFactor) {
        this.adaptor = adaptor;
        this.freeSpaceRatio = freeSpaceRatio;
        capacityGuard = initialCapacity;
        this.growthFactor = growthFactor;

        data = new Object[(int) (initialCapacity * freeSpaceRatio)];
    }

    public boolean add(Object obj) {
        return add(obj, true);
    }

    public synchronized boolean add(Object obj, boolean checkCapacity) {
        if (checkCapacity) {
            checkCapacity();
        }

        int id = adaptor.getId(obj);

        int slot = getIndex(id);

        if (data[slot] != null) {
            return false;
        }

        ++size;
        data[slot] = obj;
        return true;

    }

    synchronized void checkCapacity() {
        if (size == capacityGuard) {
            capacityGuard = (int) (capacityGuard * growthFactor);
            Object[] oldData = data;
            data = new Object[(int) (capacityGuard * freeSpaceRatio)];

            size = 0;
            for (int i = 0; i < oldData.length; i++) {
                if (oldData[i] != null) {
                    add(oldData[i], false);
                }
            }
        }

    }

    public Object get(int id) {
        int slot = getIndex(id);

        return data[slot];
    }

    /**
     * Returns index of either next available empty slot or slot with object.id ==
     * id
     *
     * In a rare case when all slots are busy and no object is found it returns
     * -1. It may happen if ratio == 1 (generally bad idea)
     *
     * @param id
     * @return
     */
    public int getIndex(int id) {
        int len = data.length;
        int start = id % len, slot = start;

        do {
            Object obj;
            if ((obj = data[slot]) == null) {
                return slot;
            }

            if (adaptor.getId(obj) == id) {
                return slot;
            }

            slot = (slot + 1) % len;
        } while (slot != start);

        return -1;
    }

    public synchronized boolean remove(int id) {
        int slot = getIndex(id);

        if (data[slot] == null) {
            return false;
        }

        data[slot] = null;
        --size;

        // replace tail elements

        int len = data.length;

        while (true) {
            slot = (slot + 1) % len;

            Object obj = data[slot];

            if (obj == null) {
                return true;
            }

            --size;
            data[slot] = null;
            add(obj, false);
        }

    }

    public boolean remove(Object obj) {
        return remove(adaptor.getId(obj));
    }

    public int size() {
        return size;
    }

}
