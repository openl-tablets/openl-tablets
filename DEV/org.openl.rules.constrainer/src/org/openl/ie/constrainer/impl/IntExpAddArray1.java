package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

/**
 * An implementation of the expression: <code>sum(IntExpArray)</code>. This implementation "remember and propagate"
 * setMin/Max.
 */
public final class IntExpAddArray1 extends IntExpImpl {
    final class DomainVar extends IntVarImpl {
        public DomainVar(Constrainer c, int min, int max) {
            super(c, min, max, "", DOMAIN_PLAIN);
        }

        @Override
        public void propagate() throws Failure {
            enforceDomainC();
        }

    } // ~DomainVar

    class ExpAddVectorObserver extends Observer {

        @Override
        public Object master() {
            return IntExpAddArray1.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "ExpAddVectorObserver: " + _vars;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            var e = (IntEvent) event;

            _domainE.setMin(_domainE.min() + e.mindiff());
            _domainE.setMax(_domainE.max() + e.maxdiff());

            // update domainC
            _domainC.setMin(_domainE.min());
            _domainC.setMax(_domainE.max());

        }

    } // ~ ExpAddVectorObserver

    private final IntExpArray _vars;

    private final Observer _observer;

    private final IntVar _domainC; // constraint domain

    private final DomainVar _domainE; // expression domain

    public IntExpAddArray1(Constrainer constrainer, IntExpArray vars) {
        super(constrainer);
        vars.size();
        _vars = vars;
        _observer = new ExpAddVectorObserver();

        var data = _vars.data();

        for (IntExp datum : data) {
            datum.attachObserver(_observer);
        }

        var sumName = "";

        if (constrainer().showInternalNames()) {
            var s = new StringBuilder();
            s.append("(");
            for (var i = 0; i < data.length; i++) {
                if (i != 0) {
                    s.append("+");
                }
                s.append(data[i].name());
            }
            s.append(")");
            _name = s.toString();

            sumName = "sum(" + _vars.name() + ")";
        }

        var min = calc_min();
        var max = calc_max();
        _domainC = constrainer().addIntVarTraceInternal(min, max, sumName, IntVar.DOMAIN_PLAIN);
        _domainE = new DomainVar(constrainer(), min, max);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _domainC.attachObserver(observer);
    }

    int calc_max() {
        var maxSum = 0;

        var vars = _vars.data();

        for (IntExp var : vars) {
            maxSum += var.max();
        }
        return maxSum;
    }

    int calc_min() {
        var minSum = 0;

        var vars = _vars.data();

        for (IntExp var : vars) {
            minSum += var.min();
        }
        return minSum;
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _domainC.detachObserver(observer);
    }

    void enforceDomainC() throws Failure {
        var minC = _domainC.min();
        var maxC = _domainC.max();
        var minE = _domainE.min();
        var maxE = _domainE.max();

        if (minC == minE && maxC == maxE) {
            return;
        }

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var mini = vari.min();
            var maxi = vari.max();

            var newMin = minC - (maxE - maxi);
            if (newMin > mini) {
                vari.setMin(newMin);
            }

            var newMax = maxC - (minE - mini);
            if (newMax < maxi) {
                vari.setMax(newMax);
            }
        }
    }

    @Override
    public boolean isLinear() {
        for (var i = 0; i < _vars.size(); i++) {
            if (!_vars.get(i).isLinear()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int max() {
        return _domainC.max();
    }

    @Override
    public int min() {
        return _domainC.min();
    }

    @Override
    public void name(String name) {
        super.name(name);
        _domainC.name(name);
    }

    @Override
    public void onMaskChange() {
        // The sum does not depend on the event mask.
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _domainC.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        var max = _domainC.max();
        if (value > max) {
            return;
        }
        var min = _domainC.min();
        if (value < min) {
            return;
        }
        if (min == max) {
            constrainer().fail("remove for IntExpAddVector");
        }
        if (value == max) {
            setMax(value - 1);
        }
        if (value == min) {
            setMin(value + 1);
        }
    }

    @Override
    public void setMax(int max) throws Failure {

        if (max >= _domainC.max()) {
            return;
        }

        _domainC.setMax(max);

        var minSum = _domainE.min();

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var maxi = max - (minSum - vari.min());
            if (maxi < vari.max()) {
                vari.setMax(maxi);
            }
        }
    }

    @Override
    public void setMin(int min) throws Failure {

        if (min <= _domainC.min()) {
            return;
        }

        _domainC.setMin(min);

        var maxSum = _domainE.max();

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var mini = min - (maxSum - vari.max());
            if (mini > vari.min()) {
                vari.setMin(mini);
            }
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        if (_domainC.min() == value && _domainC.max() == value) {
            return;
        }

        _domainC.setValue(value);

        var sumMin = _domainE.min();
        var sumMax = _domainE.max();

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var mini = vari.min();
            var maxi = vari.max();

            var newMin = value - (sumMax - maxi);
            if (newMin > mini) {
                vari.setMin(newMin);
            }

            var newMax = value - (sumMin - mini);
            if (newMax < maxi) {
                vari.setMax(newMax);
            }
        }
    }

    @Override
    public int size() {
        return max() - min() + 1;
    }

} // ~IntExpAddArray1
