package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

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
interface ArcConsistency {
    public void arcConsistency(IntExpArray vars) throws Failure;
}

/**
 * An implementation of the AC-1 algorithm. Experimental release.
 */
public class ArcConsistency_1 implements ArcConsistency {
    /**
     *
     */
    static public final class Bitset {
        private boolean[] _bits;
        private int _initial_size;
        private int _initial_min;
        private int _initial_max;
        private int _size;
        private int _min;
        private int _max;

        public Bitset(IntExp exp) {
            _max = _initial_max = exp.max();
            _min = _initial_min = exp.min();
            _bits = new boolean[_initial_max - _initial_min + 1];

            _initial_size = 0;
            for (int i = 0; i < _bits.length; ++i) {
                if (exp.contains(_initial_min + i)) {
                    _bits[i] = true;
                    _initial_size++;
                } else {
                    _bits[i] = false;
                }
            }
            _size = _initial_size;
        }

        public boolean contains(int value) {
            if (value < _min || value > _max) {
                return false;
            }

            return _bits[value - _initial_min];
        }

        public int initialSize() {
            return _initial_size;
        }

        public boolean isEmpty() {
            return _size == 0;
        }

        public int max() {
            return _max;
        }

        public int min() {
            return _min;
        }

        public int next(int value) {
            int i = value + 1;
            if (i < _min || i > _max) {
                return value;
            }

            for (; i <= _max; ++i) {
                if (_bits[i - _initial_min]) {
                    return i;
                }
            }

            return value;
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

        public boolean removeValue(int value) {
            if (!contains(value)) {
                return false;
            }

            if (value == _min) {
                _min = value + 1;
            }

            if (value == _max) {
                _max = value - 1;
            }

            _bits[value - _initial_min] = false;
            --_size;

            return true;
        }

        public int size() {
            return _size;
        }

        @Override
        public String toString() {
            return "[" + printIntervals() + "]";
        }

        int upperBound(int i) {
            if (i > _max) {
                return i;
            }
            boolean sample = _bits[i - _initial_min];

            for (int j = i;; ++j) {
                if (j > _max || _bits[j - _initial_min] != sample) {
                    return j;
                }
            }
        }

    } // ~Bitset
    /**
     *
     */
    static public final class BitsetIterator {
        private Bitset _bits;
        private int _value;

        /**
         * Default constructor. Note: default constructor creates the iterator
         * in invalid state. Use reset(Bitset) to make the iterator valid.
         */
        public BitsetIterator() {
        }

        public BitsetIterator(Bitset bits) {
            reset(bits);
        }

        public boolean hasNext() {
            return _bits.next(_value) != _value;
        }

        public int next() {
            if (!hasNext()) {
                throw new RuntimeException("BitsetIterator.next()");
            }
            _value = _bits.next(_value);
            return _value;
        }

        public void reset() {
            _value = _bits.min() - 1;
        }

        public void reset(Bitset bits) {
            _bits = bits;
            _value = _bits.min() - 1;
        }

    } // ~BitsetIterator
    private class ReviseIJ extends GoalImpl {
        private BitsetIterator _itI = new BitsetIterator();
        private BitsetIterator _itJ = new BitsetIterator();
        private int _I, _i, _J, _j;

        public ReviseIJ(Constrainer c) {
            super(c);
        }

        public boolean check_i() {
            _itJ.reset(_domains[_J]);
            while (_itJ.hasNext()) {
                _j = _itJ.next();
                if (constrainer().execute(this, true)) {
                    return true;
                }
            }
            return false;
        }

        public Goal execute() throws Failure {
            _total_checks++;
            _vars.elementAt(_I).setValue(_i);
            _vars.elementAt(_J).setValue(_j);
            return null;
        }

        public boolean revise(int I, int J) {
            _I = I;
            _J = J;
            int removed = 0;
            _itI.reset(_domains[I]);
            while (_itI.hasNext()) {
                _i = _itI.next();
                if (!check_i()) {
                    _domains[I].removeValue(_i);
                    removed++;
                }
            }
            return removed > 0;
        }

    } // ~ReviseIJ
    private IntExpArray _vars;

    private Bitset _domains[];

    private ReviseIJ _reviseIJ;

    private int _total_checks;

    static Bitset[] createDomains(IntExpArray vars) {
        Bitset[] domains = new Bitset[vars.size()];
        for (int i = 0; i < vars.size(); ++i) {
            domains[i] = new Bitset(vars.elementAt(i));
        }
        return domains;
    }

    public static long productInitialSize(Bitset[] ary) {
        int size = ary.length;
        if (size == 0) {
            return 0;
        }

        long total = 1;
        for (int i = 0; i < size; ++i) {
            total *= ary[i].initialSize();
        }
        return total;
    }

    public static long productSize(Bitset[] ary) {
        int size = ary.length;
        if (size == 0) {
            return 0;
        }

        long total = 1;
        for (int i = 0; i < size; ++i) {
            total *= ary[i].size();
        }
        return total;
    }

    public void arcConsistency(IntExpArray vars) throws Failure {
        init(vars);
        try {
            revise();
        } catch (Failure f) {
            cleanup();
            throw f;
        }
        cleanup();
    }

    void cleanup() {
        _vars = null;
        _domains = null;
        _reviseIJ = null;
    }

    void init(IntExpArray vars) {
        _vars = vars;
        _domains = createDomains(_vars);
        _reviseIJ = new ReviseIJ(vars.constrainer());
        _total_checks = 0;
    }

    void reduceDomains() throws Failure {
        for (int I = 0; I < _vars.size(); ++I) {
            IntExp exp = _vars.elementAt(I);
            for (int i = exp.min(); i <= exp.max(); ++i) {
                if (!_domains[I].contains(i)) {
                    exp.removeValue(i);
                }
            }
        }
    }

    void revise() throws Failure {
        long t_start = System.currentTimeMillis();
        int size = _vars.size();
        boolean change;
        do {
            change = false;
            for (int I = 0; I < size; ++I) {
                for (int J = 0; J < size; ++J) {
                    if (I == J) {
                        continue;
                    }
                    change |= _reviseIJ.revise(I, J);
                }
            }
        } while (change);

        long t = System.currentTimeMillis() - t_start;
        long t1 = productInitialSize(_domains), t2 = productSize(_domains);
        if (t1 != t2) {
            System.out.println("AC_1: t=" + t + "msec checks=" + _total_checks + " initial_cards=" + t1
                    + " reduced_cards=" + t2);
        } else {
            System.out.println("AC_1: t=" + t + "msec checks=" + _total_checks + " initial_cards=" + t1
                    + " NO reduction");
        }

        reduceDomains();
    }

} // ~ArcConsistency_1
