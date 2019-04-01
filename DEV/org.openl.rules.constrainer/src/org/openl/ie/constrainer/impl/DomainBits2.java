package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

/**
 * An implementation of the array of bits.
 */
final class BitArray implements java.io.Serializable {

    final static int BITS_PER_WORD = 32;

    int[] _bits;
    int _size;

    BitArray(int size) {
        _bits = new int[(size - 1) / BITS_PER_WORD + 1];
        _size = size;
        for (int i = 0; i < _bits.length; ++i) {
            _bits[i] = 0xffffffff;
        }
    }

    boolean at(int index) {
        // return (_bits[index/BITS_PER_WORD] & (1 << index % BITS_PER_WORD)) !=
        // 0 ;
        return (_bits[index / BITS_PER_WORD] & (1 << index)) != 0;
    }

    void set(int index, boolean val) {
        if (val) {
            // _bits[index/BITS_PER_WORD] |= (1 << index % BITS_PER_WORD);
            _bits[index / BITS_PER_WORD] |= (1 << index);
        } else {
            // _bits[index/BITS_PER_WORD] &= ~(1 << index % BITS_PER_WORD);
            _bits[index / BITS_PER_WORD] &= ~(1 << index);
        }
    }

    void setBits(int[] bits) {
        _bits = bits;
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
    // private IntVar _variable;
    // private boolean[] _bits;
    private int _size;
    // private int _initial_min;
    // private int _initial_max;
    // private int _min; // the first i with _bits[i]=true
    // private int _max; // the last i with _bits[i]=true

    // public static final int MAX_VALUE = 1000000;//Integer.MAX_VALUE-1;
    BitArray _bits;

    static void test1(String args[]) throws Exception {
        BitArray b = new BitArray(100);

        for (int i = 0; i < 100; ++i) {
            System.out.println(b._bits[0]);
            System.out.println("" + i + " : " + (1 << i));
            if (!b.at(i)) {
                throw new Exception("xx" + i);
            }
        }

    }

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

    void checkX(String s) {
        if (!_bits.at(_min - _initial_min)) {
            Constrainer.abort("From:" + s + "!_bits[_min - _initial_min] " + this);
        }

        if (!_bits.at(_max - _initial_min)) {
            Constrainer.abort("From:" + s + "  !_bits[_max - _initial_min] " + this);
        }
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

    String printBits() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < _bits.size(); ++i) {
            if (_bits.at(i)) {
                if (_initial_min + i < _min || _initial_min + i > _max) {
                    buf.append('x');
                } else {
                    buf.append('X');
                }
            } else {
                if (_initial_min + i < _min || _initial_min + i > _max) {
                    buf.append('o');
                } else {
                    buf.append('O');
                }
            }
        }
        return buf.toString();
    }

    String printIntervals() {
        StringBuilder buf = new StringBuilder();
        for (int i = _min; i <= _max;) {
            if (i != _min) {
                buf.append(" ");
            }
            int from = i;
            int to = upperBound(from);

            if (to - from == 1) {
                buf.append(String.valueOf(from));
            } else {
                buf.append(from).append("..").append(to - 1);
            }

            i = upperBound(to);
        }

        return buf.toString();
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

        for (; _max > M;) {
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

        for (; _min < m;) {
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
    public String toString() {
        return "[" + printIntervals() + "]";
    }

    @Override
    public int type() {
        return IntVar.DOMAIN_BIT_SMALL;
    }

    int upperBound(int i) {
        if (i > _max) {
            return i;
        }
        boolean sample = _bits.at(i - _initial_min);

        for (int j = i;; ++j) {
            if (j > _max || _bits.at(j - _initial_min) != sample) {
                return j;
            }
        }
    }

} // ~DomainBits2
