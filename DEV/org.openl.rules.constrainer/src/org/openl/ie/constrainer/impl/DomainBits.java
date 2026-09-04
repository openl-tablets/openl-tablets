package org.openl.ie.constrainer.impl;

import java.util.Arrays;

import org.openl.ie.constrainer.Domain;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

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
//
//: DomainBits.java
//

/**
 * An implementation of the Domain interface as a bit field.
 *
 * @see IntVar
 * @see Domain
 */
public final class DomainBits extends DomainImpl {
    private boolean[] _bits;
    private int _size;

    public DomainBits(IntVar var, int min, int max) // throws Failure
    {
        super(var, min, max);
        _bits = new boolean[max - min + 1];
        Arrays.fill(_bits, true);
        _size = _max - _min + 1;
    }

    public boolean[] bits() {
        return _bits;
    }

    @Override
    public boolean contains(int value) {

        if (value < _min || value > _max) {
            return false;
        }

        return _bits[value - _initial_min];
    }

    public void forceBits(boolean[] bits) {
        _bits = bits;
    }

    @Override
    public void forceInsert(int val) {
        _bits[val - _initial_min] = true;
    }

    @Override
    public void forceSize(int val) {
        _size = val;
    }

    @Override
    public void iterateDomain(IntExp.IntDomainIterator it) throws Failure {
        for (var i = _min - _initial_min; i <= _max - _initial_min; ++i) {
            if (_bits[i]) {
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
        var buf = new StringBuilder();
        for (var i = 0; i < _bits.length; ++i) {
            if (_bits[i]) {
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
        var buf = new StringBuilder();
        for (var i = _min; i <= _max; ) {
            if (i != _min) {
                buf.append(" ");
            }
            var from = i;
            var to = upperBound(from);

            if (to - from == 1) {
                buf.append(from);
            } else {
                buf.append(from).append("..").append(to - 1);
            }

            i = upperBound(to);
        }

        return buf.toString();
    }

    /**
     * added by SV 20.01.03
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
        var is_removed = false;
        for (var i = min; i <= max; i++) {
            if (contains(i)) {
                _variable.addUndo();
                _bits[i - _initial_min] = false;
                --_size;
                is_removed = true;
            }
        }
        return is_removed;
    }

    @Override
    public boolean removeValue(int value) throws Failure {

        if (!contains(value)) {
            return false;
        }

        if (value == _min) {
            return setMin(value + 1);
        }
        if (value == _max) {
            return setMax(value - 1);
        }

        _variable.addUndo();

        _bits[value - _initial_min] = false;
        --_size;

        return true;
    }

    @Override
    public boolean setMax(int M) throws Failure {
        if (M >= _max) {
            return false;
        }

        if (M < _min) {
            constrainer().fail("Max < Min ");
        }

        _variable.addUndo();

        while (_max > M) {
            if (_bits[_max-- - _initial_min]) {
                --_size;
            }
        }

        for (var i = _max - _initial_min; i >= 0 && !_bits[i]; i--) {
            if (--_max < _min) {
                constrainer().fail("max");
            }
        }

        return true;

    }

    @Override
    public boolean setMin(int m) throws Failure {
        if (m <= _min) {
            return false;
        }

        if (m > _max) {
            constrainer().fail("Min > Max ");
        }

        _variable.addUndo();

        while (_min < m) {
            if (_bits[_min++ - _initial_min]) {
                --_size;
            }
        }

        for (var i = _min - _initial_min; i < _bits.length && !_bits[i]; i++) {
            if (++_min > _max) {
                constrainer().fail("min");
            }
        }

        return true;

    }

    @Override
    public boolean setValue(int value) throws Failure {
        if (_min == value && _max == value) {
            return false;
        }

        if (!contains(value)) {
            constrainer().fail("attempt to set invalid value");
        }

        _variable.addUndo();

        _min = value;
        _max = value;
        _size = 1;
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
        return IntVar.DOMAIN_BIT_FAST;
    }

    int upperBound(int i) {
        if (i > _max) {
            return i;
        }
        var sample = _bits[i - _initial_min];

        for (var j = i; ; ++j) {
            if (j > _max || _bits[j - _initial_min] != sample) {
                return j;
            }
        }
    }

} // end of Domain
