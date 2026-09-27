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
 * An implementation of the expression: <code>sum(IntExpArray)</code>.
 */
public final class IntExpAddArray extends IntExpImpl {
    class ExpAddVectorObserver extends Observer {

        @Override
        public Object master() {
            return IntExpAddArray.this;
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

            _sum.setMin(_sum.min() + e.mindiff());
            _sum.setMax(_sum.max() + e.maxdiff());

        }

    } // ~ ExpAddVectorObserver

    private final IntExpArray _vars;

    private final Observer _observer;

    private final IntVar _sum;

    public IntExpAddArray(Constrainer constrainer, IntExpArray vars) {
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

        _sum = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), sumName, IntVar.DOMAIN_PLAIN);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _sum.attachObserver(observer);
    }

    public int calc_max() {
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
        _sum.detachObserver(observer);
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
        return _sum.max();
    }

    @Override
    public int min() {
        return _sum.min();
    }

    @Override
    public void name(String name) {
        super.name(name);
        _sum.name(name);
    }

    @Override
    public void onMaskChange() {
        // The sum does not depend on the event mask.
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _sum.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        var max = max();
        if (value > max) {
            return;
        }
        var min = min();
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

        if (max >= max()) {
            return;
        }

        var minSum = min();

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

        if (min <= min()) {
            return;
        }

        var maxSum = max();

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
        var sumMin = min();
        var sumMax = max();

        if (value < sumMin || value > sumMax) {
            _constrainer.fail("Add Array Set Value");
        }

        if (value == sumMin) {
            setMax(value);
            return;
        }
        if (value == sumMax) {
            setMin(value);
            return;
        }

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

} // ~IntExpAddArray
