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

        ExpAddVectorObserver() {
        }

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

        var sum_name = "";

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

            sum_name = "sum(" + _vars.name() + ")";
        }

        _sum = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), sum_name, IntVar.DOMAIN_PLAIN);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _sum.attachObserver(observer);
    }

    public int calc_max() {
        var max_sum = 0;

        var vars = _vars.data();

        for (IntExp var : vars) {
            max_sum += var.max();
        }
        return max_sum;
    }

    int calc_min() {
        var min_sum = 0;

        var vars = _vars.data();

        for (IntExp var : vars) {
            min_sum += var.min();
        }
        return min_sum;
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
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _sum.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        var Max = max();
        if (value > Max) {
            return;
        }
        var Min = min();
        if (value < Min) {
            return;
        }
        if (Min == Max) {
            constrainer().fail("remove for IntExpAddVector");
        }
        if (value == Max) {
            setMax(value - 1);
        }
        if (value == Min) {
            setMin(value + 1);
        }
    }

    @Override
    public void setMax(int max) throws Failure {

        if (max >= max()) {
            return;
        }

        var min_sum = min();

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var maxi = max - (min_sum - vari.min());
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

        var max_sum = max();

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var mini = min - (max_sum - vari.max());
            if (mini > vari.min()) {
                vari.setMin(mini);
            }
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        var sum_min = min();
        var sum_max = max();

        if (value < sum_min || value > sum_max) {
            _constrainer.fail("Add Array Set Value");
        }

        if (value == sum_min) {
            setMax(value);
            return;
        }
        if (value == sum_max) {
            setMin(value);
            return;
        }

        var vars = _vars.data();

        for (IntExp vari : vars) {
            var mini = vari.min();
            var maxi = vari.max();

            var new_min = value - (sum_max - maxi);
            if (new_min > mini) {
                vari.setMin(new_min);
            }

            var new_max = value - (sum_min - mini);
            if (new_max < maxi) {
                vari.setMax(new_max);
            }
        }
    }

    @Override
    public int size() {
        return max() - min() + 1;
    }

} // ~IntExpAddArray
