///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
package org.openl.ie.tools;

import java.util.Collection;
import java.util.Iterator;

// "implements serializable"  was added by Eugeny Tseitlin 18.06.2003
public final class FastVector implements Cloneable, java.io.Serializable {

    static final int DEFAULT_CAPACITY = 10;

    Object[] m_data;
    int m_size;

    public FastVector() {
        this(DEFAULT_CAPACITY);
    }

    public FastVector(Collection c) {
        this(c.size());

        Iterator it = c.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    public FastVector(int capacity) {
        m_size = 0;
        if (capacity == 0) {
            capacity = DEFAULT_CAPACITY;
        }
        m_data = new Object[capacity];
    }

    public FastVector(Object[] c) {
        this(c, 0, c.length - 1);
    }

    public FastVector(Object[] c, int fromIndex, int toIndex) {
        this(Math.max(0, toIndex - fromIndex + 1));
        if (m_size > 0) {
            System.arraycopy(m_data, fromIndex, m_data, 0, m_size);
        }
    }

    public final void add(Object obj) {
        if (m_size == m_data.length) {
            grow();
        }

        m_data[m_size++] = obj;
    }

    public final void addElement(Object obj) {
        if (m_size == m_data.length) {
            grow();
        }

        m_data[m_size++] = obj;
    }

    public void clear() {
        m_size = 0;
        m_data = new Object[m_data.length];
    }

    @Override
    public Object clone() {
        try {
            FastVector v = (FastVector) super.clone();
            v.m_data = m_data.clone();
            // v.m_data = new Object[m_data.length];
            // System.arraycopy(m_data, 0, v.m_data, 0, m_data.length);
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    public Object[] data() {
        return m_data;
    }

    public Object elementAt(int i) {
        return m_data[i];
    }

    public Object firstElement() {
        return m_data[0];
    }

    void grow() {
        Object[] old = m_data;

        m_data = new Object[m_data.length * 2];
        System.arraycopy(old, 0, m_data, 0, m_size);
    }

    public int indexOf(Object elem) {
        if (elem == null) {
            for (int i = 0; i < m_size; i++) {
                if (m_data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < m_size; i++) {
                if (elem.equals(m_data[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void insertElementAt(Object obj, int index) {
        if (m_size == m_data.length) {
            grow();
        }

        System.arraycopy(m_data, index, m_data, index + 1, m_size - index);
        m_data[index] = obj;
        m_size++;
    }

    public final boolean isEmpty() {
        return m_size == 0;
    }

    public Object lastElement() {
        return m_data[m_size - 1];
    }

    public final Object peek() {
        return m_data[m_size - 1];
    }

    public boolean removeElement(Object obj) {
        int i = indexOf(obj);
        if (i >= 0) {
            removeElementAt(i);
            return true;
        }
        return false;
    }

    public void removeElementAt(int index) {
        int j = m_size - index - 1;
        if (j > 0) {
            System.arraycopy(m_data, index + 1, m_data, index, j);
        }
        m_size--;
        m_data[m_size] = null; /* to let gc do its work */
    }

    public final Object removeLast() {
        return m_data[--m_size];
    }

    public int size() {
        return m_size;
    }

    /**
     * Returns an array containing all of the elements in this Vector in the correct order.
     *
     * @see java.util.Vector#toArray()
     */
    public Object[] toArray() {
        Object[] result = new Object[m_size];
        System.arraycopy(m_data, 0, result, 0, m_size);
        return result;
    }

    /**
     * Returns an array containing all of the elements in this Vector in the correct order. The runtime type of the
     * returned array is that of the specified array. If the Vector fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the specified array and the size of this Vector.
     * <p>
     *
     * @see java.util.Vector#toArray(Object[])
     */
    public Object[] toArray(Object a[]) {
        if (a.length < m_size) {
            a = (Object[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), m_size);
        }

        System.arraycopy(m_data, 0, a, 0, m_size);

        if (a.length > m_size) {
            a[m_size] = null;
        }

        return a;
    }

}
