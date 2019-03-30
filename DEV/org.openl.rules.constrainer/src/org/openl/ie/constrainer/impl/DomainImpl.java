package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Domain;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

//
//: DomainImpl.java
//
/**
 * An implementation of the Domain interface that supports plain domain. This implementation keeps only min/max values.
 *
 * @see IntVar
 */
public class DomainImpl implements Domain {
    protected IntVar _variable;
    protected int _initial_min;
    protected int _initial_max;
    protected int _min;
    protected int _max;

    public DomainImpl(IntVar var, int min, int max)// throws Failure
    {
        _variable = var;
        _initial_min = min;
        _initial_max = max;
        // int size = max-min+1;
        // if (size<1) throw new Failure("max < min?");
        _min = min;
        _max = max;
    }

    public Constrainer constrainer() {
        return _variable.constrainer();
    }

    public boolean contains(int value) {
        return (value >= _min && value <= _max);
    }

    public void forceInsert(int val) {
    }

    /*
     * public Set set() { Set elements = new HashSet(); for(int i=0; i<_values.size(); i++) { DomainInterval interval =
     * (DomainInterval)_values.elementAt(i); for(int v=interval.from; v<=interval.to; ++v) { elements.add(new
     * Integer(v)); } } return elements; }
     */

    public void forceMax(int max) {
        _max = max;
        // check("forceMax(" + max + ")");
    }

    public void forceMin(int min) {
        _min = min;
    }

    public void forceSize(int val) {
    }

    public void iterateDomain(IntExp.IntDomainIterator it) throws Failure {
        for (int i = _min; i <= _max; ++i) {
            if (!it.doSomethingOrStop(i)) {
                return;
            }
        }
    }

    public int max() {
        return _max;
    }

    public int min() {
        return _min;
    }

    /**
     * Does nothing
     *
     * added by SV 20.01.03
     *
     * @param min
     * @param max
     * @throws Failure
     */
    public boolean removeRange(int min, int max) throws Failure {
        boolean is_removed = false;
        if (min <= _min && max >= _max) {
            constrainer().fail("Empty domain");
        }
        if (min <= _min && max >= _min) {
            return setMin(max + 1);
        }
        if (max >= _max && min <= _max) {
            return setMax(min - 1);
        }
        return is_removed;
    }

    // Prohibited in this implementation!
    public boolean removeValue(int value) throws Failure {
        // Debug.print(this+".removeValue("+value+")");
        if (value == min()) {
            return setMin(value + 1);
        } else if (value == max()) {
            return setMax(value - 1);
        }
        // System.out.println("Method removeValue is prohibited for this
        // implementation");
        return false;
    }

    public boolean setMax(int M) throws Failure {
        if (M >= _max) {
            return false;
        }
        if (M < _min) {
            // Debug.print("Attempt to set max to "+M+" for "+_variable);
            // if (constrainer().showFailures())
            // constrainer().fail("Attempt to set max to "+M+" for "+_variable);
            // else
            constrainer().fail("DomainImpl setMax");
        }
        // constrainer().addUndo(_variable);
        _variable.addUndo();
        _max = M;
        // check("setMax(" + M + ")");
        return true;
    }

    public boolean setMin(int m) throws Failure {
        if (m <= _min) {
            return false;
        }
        if (m > _max) {
            // Debug.print("Attempt to set min to "+m+" for "+_variable);
            // if (constrainer().showFailures())
            // constrainer().fail("Attempt to set min to "+m+" for "+_variable);
            // else
            constrainer().fail("DomainImpl setMin");
        }
        // constrainer().addUndo(_variable);
        _variable.addUndo();
        _min = m;
        // check("setMin(" + m + ")");
        return true;
    }

    public boolean setValue(int value) throws Failure {
        // Debug.print("setValue " + value);
        if (_min == value && _max == value) {
            // constrainer().fail("Redundant value "+_variable);
            return false;
        }

        if (!contains(value)) {
            // if (constrainer().showFailures())
            // constrainer().fail("Attempt to set value to "+value+" for
            // "+_variable);
            // else
            constrainer().fail("DomainImpl setValue");
        }

        // constrainer().addUndo(_variable);
        _variable.addUndo();
        _min = value;
        _max = value;
        // check("setValue(" + value + ")");
        return true;
    }

    public int size() {
        return (_max - _min + 1);
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return "[" + min() + ((size() == 1) ? "" : ";" + max()) + "]";
    }

    public int type() {
        return IntVar.DOMAIN_PLAIN;
    }

    public void variable(IntVar var) {
        _variable = var;
    }

} // ~DomainImpl
