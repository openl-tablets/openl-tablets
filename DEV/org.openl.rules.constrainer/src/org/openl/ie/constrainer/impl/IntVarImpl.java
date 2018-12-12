package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Domain;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.Undoable;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


//
//: IntVarImpl.java
//
/**
 * A generic implementation of the IntVar interface.
 */
public class IntVarImpl extends IntExpImpl implements IntVar {
    /**
     * Undo Class for IntVar.
     */
    static final class UndoIntVarImpl extends UndoSubject {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoIntVarImpl();
            }

        };

        int _history_index;

        static UndoIntVarImpl getIntVarUndo() {
            return (UndoIntVarImpl) _factory.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoIntVar " + undoable();
        }

        @Override
        public void undo() {
            IntVarImpl intvar = (IntVarImpl) undoable();
            // System.out.println("++ Undo: " + intvar);
            intvar.history().restore(_history_index);
            super.undo();
            // System.out.println("-- Undo: " + intvar);
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            IntVarImpl intvar = (IntVarImpl) u;
            _history_index = intvar.history().currentIndex();
            // System.out.println("++ SAVE: " + intvar + "index:" +
            // _history_index);

        }

    } // ~UndoIntVarImpl
    private Domain _domain;

    private IntDomainHistory _history;

    public IntVarImpl(Constrainer constrainer, int max) {
        this(constrainer, 0, max);
    }

    public IntVarImpl(Constrainer constrainer, int min, int max) {
        this(constrainer, min, max, "var");
    }

    public IntVarImpl(Constrainer constrainer, int min, int max, String name) {
        super(constrainer, name);
        _domain = new DomainBits(this, min, max);

        _history = new IntDomainHistory(this);
    }

    public IntVarImpl(Constrainer constrainer, int min, int max, String name, int domain_type) {
        super(constrainer, name);

        int size = max - min + 1;

        switch (domain_type) {
            case DOMAIN_PLAIN:
                _domain = new DomainImpl(this, min, max);
                break;
            case DOMAIN_BIT_FAST:
                _domain = new DomainBits(this, min, max);
                break;
            case DOMAIN_BIT_SMALL:
                _domain = new DomainBits2(this, min, max);
                break;
            case DOMAIN_DEFAULT:
                if (size < 16) {
                    _domain = new DomainBits(this, min, max);
                } else if (size < 128) {
                    _domain = new DomainBits2(this, min, max);
                } else {
                    _domain = new DomainImpl(this, min, max);
                }
                break;
        }

        _history = new IntDomainHistory(this);
    }

    @Override
    public boolean contains(int value) {
        return _domain.contains(value);
    }

    @Override
    public Undo createUndo() {
        _history.saveUndo();
        return UndoIntVarImpl.getIntVarUndo();
    }

    @Override
    public String domainToString() {
        return _domain.toString();
    }

    public int domainType() {
        return _domain.type();
    }

    public void forceInsert(int val) {
        _domain.forceInsert(val);
    }

    public void forceMax(int val) {
        _domain.forceMax(val);
    }

    public void forceMin(int val) {
        _domain.forceMin(val);
    }

    public void forceSize(int val) {
        _domain.forceSize(val);
    }

    public IntDomainHistory history() {
        return _history;
    }

    public Goal instantiate() {
        return new GoalInstantiate(this);
    }

    @Override
    public boolean isLinear() {
        return true;
    }

    @Override
    public void iterateDomain(IntExp.IntDomainIterator it) throws Failure {
        _domain.iterateDomain(it);
    }

    public int max() {
        return _domain.max();
    }

    public int min() {
        return _domain.min();
    }

    @Override
    public void propagate() throws Failure {
        _history.propagate();
    }

    @Override
    protected void removeRangeInternal(int min, int max) throws Failure {
        if (_domain.removeRange(min, max)) {
            _history.setMin(_domain.min());
            _history.setMax(_domain.max());
            _history.remove(min, max);
            addToPropagationQueue();
        }
    }

    // removes only min and max value (leaves holes)
    @Override
    public void removeValue(int value) throws Failure {
        if (_domain.removeValue(value)) {
            _history.setMin(_domain.min());
            _history.setMax(_domain.max());
            _history.remove(value);
            addToPropagationQueue();
        }
    }

    public void setMax(int max) throws Failure {
        if (_domain.setMax(max)) {
            _history.setMax(max());
            addToPropagationQueue();
        }
    }

    public void setMin(int min) throws Failure {
        if (_domain.setMin(min)) {
            _history.setMin(min());
            addToPropagationQueue();
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    @Override
    public int size() {
        return _domain.size();
    }

    @Override
    public int value() throws Failure {
        if (!bound()) {
            constrainer().fail("Attempt to get value of the unbound variable " + this);
        }
        return _domain.min();
    }

} // ~IntVarImpl
