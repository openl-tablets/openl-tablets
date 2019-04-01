package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.tools.FastVector;

//
//: DomainImplWithHoles.java
//
/**
 * A implementation of the Domain interface that supports a vector of intervals where each interval defines the only
 * possible values.
 *
 * @see DomainInterval
 */
public final class DomainImplWithHoles extends DomainImpl {
    private FastVector _values; // vector of DomainUnterval(s), for example
    // domain {0,1,2,5,6,9}

    // contains 3 intervals [(0;2), (5;6), (9;9)]

    public DomainImplWithHoles(IntVar var, int min, int max) // throws
    // Failure
    {
        super(var, min, max);
        _values = new FastVector();
        _values.addElement(new DomainInterval(min, max));
    }

    @Override
    public boolean contains(int value) {
        if (value < _min || value > _max) {
            return false;
        }
        for (int i = 0; i < _values.size(); i++) {
            DomainInterval interval = (DomainInterval) _values.elementAt(i);
            if (value >= interval.from && value <= interval.to) {
                return true;
            }
        }
        return false;
    }

    public void force(FastVector values) // throws Failure
    {
        _values = values;
        DomainInterval first = (DomainInterval) _values.firstElement();
        _min = first.from;
        DomainInterval last = (DomainInterval) _values.lastElement();
        _max = last.to;
    }

    @Override
    public void forceMax(int max) {
        _max = max;
    }

    @Override
    public void forceMin(int min) {
        _min = min;
    }

    @Override
    public int max() {
        // DomainInterval interval = (DomainInterval)_values.lastElement();
        // return interval.to;
        return _max;
    }

    @Override
    public int min() {
        // DomainInterval interval = (DomainInterval)_values.firstElement();
        // return interval.from;
        return _min;
    }

    @Override
    public boolean removeValue(int value) throws Failure {
        if (value == _min) {
            return setMin(value + 1);
        }
        if (value == _max) {
            return setMax(value - 1);
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        for (int i = 0; i < _values.size(); i++) {
            DomainInterval interval = (DomainInterval) _values.elementAt(i);
            if (value >= interval.from && value <= interval.to) {
                if (interval.from == interval.to) {
                    if (_values.size() == 1) {
                        constrainer().fail("remove"); // "Empty domain of
                        // "+_variable
                    }
                    _values.removeElementAt(i);
                } else if (value == interval.from) {
                    interval.from++;
                } else if (value == interval.to) {
                    interval.to--;
                } else {
                    int from1 = interval.from;
                    int to1 = value - 1;
                    int from2 = value + 1;
                    int to2 = interval.to;
                    interval.to = to1;
                    _values.insertElementAt(new DomainInterval(from2, to2), i + 1);
                }
                return true;
            }
        }
        return false; // not in domain - impossible
    }

    @Override
    public boolean setMax(int M) throws Failure {
        if (M >= _max) {
            return false;
        }

        if (M < _min) {
            constrainer().fail("Max < Min for " + _variable);
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        // remove a hole
        while (!_values.isEmpty()) {
            DomainInterval interval = (DomainInterval) _values.lastElement();
            if (M < interval.from) {
                _values.removeLast();
                continue;
            }

            if (M >= interval.to) {
                break;
            }

            // (M >= interval.from && M < interval.to)
            interval.to = M;
            break;
        }

        DomainInterval interval = (DomainInterval) _values.lastElement();
        _max = interval.to;

        return true;
    }

    @Override
    public boolean setMin(int m) throws Failure {
        if (m <= min()) {
            return false;
        }

        if (m > max()) {
            constrainer().fail("Min > Max for " + _variable);
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        // remove hole
        while (!_values.isEmpty()) {
            DomainInterval interval = (DomainInterval) _values.firstElement();
            if (m > interval.to) {
                _values.removeElementAt(0);
                continue;
            }

            if (m <= interval.from) {
                break;
            }

            // (m > interval.from && m <= interval.to)
            interval.from = m;
            break;
        }

        DomainInterval interval = (DomainInterval) _values.firstElement();
        _min = interval.from;
        return true;
    }

    @Override
    public boolean setValue(int value) throws Failure {
        // Debug.print("setValue " + value);
        if (_min == value && _max == value) {
            // constrainer().fail("Redundant value "+_variable);
            return false;
        }

        if (!contains(value)) {
            constrainer().fail("attempt to set invalid value for " + _variable);
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();

        _values.clear();
        _values.addElement(new DomainInterval(value, value));
        _min = value;
        _max = value;
        return true;
    }

    @Override
    public int size() {
        int s = 0;
        for (int i = 0; i < _values.size(); i++) {
            DomainInterval interval = (DomainInterval) _values.elementAt(i);
            s += (interval.to - interval.from + 1);
        }
        return s;
    }

    @Override
    public String toString() {
        // return "["+min()+((size()==1) ? "" : ";"+max())+"]"
        // +((values().size()==1)?"":"-"+values().size()+"intervals");
        return _values.toString();
    }

    FastVector values() {
        return _values;
    }

} // end of DomainImplWithHoles
