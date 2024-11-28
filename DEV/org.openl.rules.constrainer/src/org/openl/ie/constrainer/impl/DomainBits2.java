package org.openl.ie.constrainer.impl;

import java.io.Serializable;
import java.util.Arrays;

import org.openl.ie.constrainer.Domain;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

/**
 * An implementation of the array of bits.
 */
final class BitArray implements Serializable {

    private static final int BITS_PER_WORD = 32;

    int[] _bits;
    final int _size;

    BitArray(int size) {
        _bits = new int[(size - 1) / BITS_PER_WORD + 1];
        _size = size;
        Arrays.fill(_bits, 0xffffffff);
    }

    boolean at(int index) {
        // return (_bits[index/BITS_PER_WORD] & (1 << index % BITS_PER_WORD)) !=
        // 0 ;
        return (_bits[index / BITS_PER_WORD] & 1 << index) != 0;
    }

    void set(int index, boolean val) {
        if (val) {
            // _bits[index/BITS_PER_WORD] |= (1 << index % BITS_PER_WORD);
            _bits[index / BITS_PER_WORD] |= 1 << index;
        } else {
            // _bits[index/BITS_PER_WORD] &= ~(1 << index % BITS_PER_WORD);
            _bits[index / BITS_PER_WORD] &= ~(1 << index);
        }
    }

    int size() {
        return _size;
    }

} // ~BitArray

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein is the property of Exigen Group, USA. All rights reserved. The
 * program(s) may be used and/or copied only with the written permission of Exigen Group or in accordance with the terms
 * and conditions stipulated in the agreement/contract under which the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
//
// : DomainBits2.java
//

/**
 * An implementation of the Domain interface as a small bit field.
 *
 * @see IntVar
 * @see Domain
 */
public final class DomainBits2 extends DomainImpl {
    private int _size;
    final BitArray _bits;

    public DomainBits2(IntVar var, int min, int max) // throws Failure
    {
        super(var, min, max);
        _size = _max - _min + 1;
        _bits = new BitArray(_size);
        // check("constructor");
    }

    public int[] bits() {
        return _bits._bits;
    }

    @Override
    public boolean contains(int value) {

        if (value < _min || value > _max) {
            return false;
        }

        return _bits.at(value - _initial_min);
    }

    /*
     * catch(Exception ex) { System.out.println("Error: length: " + _bits.length + " _initialMin: " + _initial_min + "
     * _initialMax: " + _initial_max + " _min: " + _min + " _max: " + _max + " value: " + value ); System.exit(1);
     * return false; }
     */

    public void forceBits(int[] bits) {
        _bits._bits = bits;
    }

    @Override
    public void forceInsert(int val) {
        _bits.set(val - _initial_min, true);
    }

    @Override
    public void forceSize(int size) {
        _size = size;
    }

    @Override
    public void iterateDomain(IntExp.IntDomainIterator it) throws Failure {
        for (int i = _min - _initial_min; i <= _max - _initial_min; ++i) {
            if (_bits.at(i)) {
                if (!it.doSomethingOrStop(i + _initial_min)) {
                    return;
                }

            }
        }
    }

    @Override
    public int max() {
        return _max;
    }

    @Override
    public int min() {
        return _min;
    }

    /**
     * added by SV 20.01.03
     *
     * @param min
     * @param max
     * @throws Failure
     */
    @Override
    public boolean removeRange(int min, int max) throws Failure {
        if (min <= _min && max >= _max) {
            constrainer().fail("Empty domain");
        }
        if (min <= _min && max >= _min) {
            return setMin(max + 1);
        } else if (max >= _max && min <= _max) {
            return setMax(min - 1);
        }
        boolean is_removed = false;
        for (int i = min; i <= max; i++) {
            if (contains(i)) {
                _variable.addUndo();
                _bits.set(i - _initial_min, false);
                --_size;
                is_removed = true;
            }
        }
        return is_removed;
    }

    @Override
    public boolean removeValue(int value) throws Failure {

        // System.out.println("Before Remove: " + value + " this=" + this);
        if (!contains(value)) {
            return false;
        }

        /*
         * if (size() <= 1) { constrainer().fail("remove"); }
         *
         */

        if (value == _min) {
            return setMin(value + 1);
        }
        if (value == _max) {
            return setMax(value - 1);
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        _bits.set(value - _initial_min, false);
        --_size;

        // System.out.println("After Remove: " + value + " this=" + this);
        return true;
    }

    @Override
    public boolean setMax(int M) throws Failure {
        if (M >= _max) {
            return false;
        }

        if (M < _min) {
            constrainer().fail("Max < Min");
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        // _max = M;

        while (_max > M) {
            if (_bits.at(_max-- - _initial_min)) {
                --_size;
            }
        }

        for (int i = _max - _initial_min; i >= 0 && !_bits.at(i); i--) {
            if (--_max < _min) {
                constrainer().fail("max");
            }
        }

        // check("setMax(" + M + ")");
        return true;

    }

    /*
     * public String toString() { return "[" + _initial_min + ":" + _min + ";" + _max + ":" + _initial_max + "]" +
     * " bits: " + printBits() + " size: " + size(); }
     */

    @Override
    public boolean setMin(int m) throws Failure {
        if (m <= _min) {
            return false;
        }

        if (m > _max) {
            constrainer().fail("Min > Max");
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        // _min = m;

        while (_min < m) {
            if (_bits.at(_min++ - _initial_min)) {
                --_size;
            }
        }

        for (int i = _min - _initial_min; i < _bits.size() && !_bits.at(i); i++) {
            if (++_min > _max) {
                constrainer().fail("min");
            }
        }

        // check("setMin(" + m + ")");
        return true;

    }

    @Override
    public boolean setValue(int value) throws Failure {
        if (_min == value && _max == value) {
            // constrainer().fail("Redundant value "+_variable);
            return false;
        }

        if (!contains(value)) {
            constrainer().fail("attempt to set invalid value");
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        _min = value;
        _max = value;
        _size = 1;
        // check("setValue(" + value + ")");
        return true;
    }

    @Override
    public int size() {
        return _size;
    }

    @Override
    public int type() {
        return IntVar.DOMAIN_BIT_SMALL;
    }

} // ~DomainBits2
