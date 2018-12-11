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

// "implements serializable"  was added by Eugeny Tseitlin 18.06.2003
public final class FastVectorInt implements Cloneable, java.io.Serializable {

    static final int DEFAULT_CAPACITY = 10;

    int[] m_data;
    int m_size;

    public FastVectorInt(int capacity) {
        m_size = 0;
        if (capacity == 0) {
            capacity = DEFAULT_CAPACITY;
        }
        m_data = new int[capacity];
    }

    public final void add(int val) {
        if (m_size == m_data.length) {
            grow();
        }

        m_data[m_size++] = val;
    }

    public void clear() {
        m_size = 0;
        // m_data = new Object[m_data.length];
    }

    @Override
    public Object clone() {
        try {
            FastVectorInt v = (FastVectorInt) super.clone();
            v.m_data = m_data.clone();
            // v.m_data = new int[m_data.length];
            // System.arraycopy(m_data, 0, v.m_data, 0, m_data.length);
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    public void cutSize(int new_size) {
        m_size = new_size;
    }

    public int elementAt(int i) {
        return m_data[i];
    }

    void grow() {
        int[] old = m_data;

        m_data = new int[m_data.length * 2];
        System.arraycopy(old, 0, m_data, 0, m_size);
    }

    public final boolean isEmpty() {
        return m_size == 0;
    }

    public final int peek() {
        return m_data[m_size - 1];
    }

    public int size() {
        return m_size;
    }
}
