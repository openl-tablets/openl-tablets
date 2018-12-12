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

/**
 * class FastQueue implements first-in first-out algorithm
 */

// "implements serializable" was added by Eugeny Tseitlin 18.06.2003
public final class FastQueue implements java.io.Serializable {

    static final int DEFAULT_CAPACITY = 10;

    static final int DEFAULT_FREE_SPACE_GROW_FACTOR = 10; // %
    // free < capacity * m_grow_factor / 100 >> grow
    int m_grow_factor = DEFAULT_FREE_SPACE_GROW_FACTOR;

    Object[] m_data;
    int m_last = 0;
    int m_first = 0;

    public FastQueue() {
        this(DEFAULT_CAPACITY, DEFAULT_FREE_SPACE_GROW_FACTOR);
    }

    public FastQueue(int capacity, int grow_factor) {
        if (capacity <= 0) {
            capacity = DEFAULT_CAPACITY;
        }
        if (grow_factor == 0) {
            grow_factor = DEFAULT_FREE_SPACE_GROW_FACTOR;
        }
        m_data = new Object[capacity];
    }

    public void clear() {
        m_last = m_first = 0;
        m_data = new Object[m_data.length];
    }

    @Override
    public Object clone() {
        try {
            FastQueue v = (FastQueue) super.clone();
            v.m_data = m_data.clone();
            // v.m_data = new Object[m_data.length];
            // System.arraycopy(m_data, 0, v.m_data, 0, m_data.length);
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    public boolean empty() {
        return m_last == m_first;
    }

    void grow() {
        Object[] old_data = m_data;
        Object[] new_data = m_data;

        if (m_first < m_data.length * m_grow_factor / 100) {
            new_data = new Object[m_data.length * 2];
        }

        if (m_last > m_first) {
            System.arraycopy(old_data, m_first, new_data, 0, m_last - m_first);
        }
        m_data = new_data;
        m_last -= m_first;
        m_first = 0;
    }

    public Object peek() {
        return m_data[m_first];
    }

    public Object pop() {
        return m_data[m_first++];
    }

    public void push(Object obj) {
        if (m_last == m_data.length) {
            grow();
        }

        m_data[m_last++] = obj;
    }

    public int size() {
        return m_last - m_first;
    }
}
