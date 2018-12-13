package org.openl.ie.constrainer.impl;

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
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.tools.FastVectorDouble;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/*
 * Changes:
 * 02.20.03 Constrainer.FLOAT_PRECISION is added by SV
 */
/**
 * An implementation of the history for the floating-point domain.
 */
public final class FloatDomainHistory {
    /**
     * An implementation of the event about change in the floating-point domain.
     */
    static final class FloatEventDomain extends FloatEvent {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventDomain();
            }
        };

        protected double _min, _max, _oldmin, _oldmax;

        protected int _type_mask;
        FloatDomainHistory _history;
        static FloatEventDomain getEvent(FloatDomainHistory history) {
            FloatEventDomain ev = (FloatEventDomain) _factory.getElement();
            ev.init(history);
            return ev;
        }

        public void init(FloatDomainHistory hist) {
            exp(hist._var);
            _min = hist.min();
            _max = hist.max();
            _oldmin = hist.oldmin();
            _oldmax = hist.oldmax();
            _type_mask = hist._mask;

            _history = hist;
        }

        @Override
        public double max() {
            return _max;
        }

        @Override
        public double min() {
            return _min;
        }

        @Override
        public String name() {
            return "FloatEventDomain";
        }

        @Override
        public double oldmax() {
            return _oldmax;
        }

        @Override
        public double oldmin() {
            return _oldmin;
        }

        @Override
        public int type() {
            return _type_mask;
        }
    }
    final static int MIN_IDX = 0;
    final static int MAX_IDX = 1;

    final static int LAST_IDX = 2;
    FloatVar _var;
    double _min;

    double _max;
    int _mask;
    FastVectorDouble _history;

    int _currentIndex = -1;

    public FloatDomainHistory(FloatVar var) {
        _var = var;
        _history = new FastVectorDouble(10);
        save();
    }

    public int currentIndex() {
        return _currentIndex;
    }

    public double max() {
        return _max;
    }

    public double min() {
        return _min;
    }

    public double oldmax() {
        return _history.elementAt(_currentIndex + MAX_IDX);
    }

    public double oldmin() {
        return _history.elementAt(_currentIndex + MIN_IDX);
    }

    void propagate() throws Failure {
        if ((_var.publisherMask() & _mask) != 0) {
            FloatEventDomain ev = FloatEventDomain.getEvent(this);
            save();
            _var.notifyObservers(ev);
        } else {
            save();
        }
    }

    public void restore(int index) {
        _var.forceMin(_min = _history.elementAt(index + MIN_IDX));
        _var.forceMax(_max = _history.elementAt(index + MAX_IDX));
        _history.cutSize(index + LAST_IDX);
        _currentIndex = index;
        _mask = 0;
    }

    int save() {
        int old = _currentIndex;
        _currentIndex = _history.size();
        _history.add(_min = _var.min());
        _history.add(_max = _var.max());
        _mask = 0;
        return old;
    }

    public void saveUndo() {
        if (_mask != 0) {
            save();
        }
    }

    void setMax(double val) {
        if (val < _max - Constrainer.FLOAT_PRECISION) {
            _max = val;
            _mask |= EventOfInterest.MAX;
            if (_var.bound()) {
                _mask |= EventOfInterest.VALUE;
            }
        }
    }

    void setMin(double val) {
        if (val > _min + Constrainer.FLOAT_PRECISION) {
            _min = val;
            _mask |= EventOfInterest.MIN;
            if (_var.bound()) {
                _mask |= EventOfInterest.VALUE;
            }
        }
    }

    @Override
    public String toString() {
        return "History: " + _history + ":" + _currentIndex + "(" + _min + "-" + _max + ")" + "mask: " + _mask;
    }
}