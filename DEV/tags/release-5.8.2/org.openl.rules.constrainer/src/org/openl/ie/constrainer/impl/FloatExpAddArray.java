package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * An implementation of the expression: <code>sum(FloatExpArray)</code>.
 */
public final class FloatExpAddArray extends FloatExpImpl {
    class FloatExpAddVectorObserver extends Observer {

        FloatExpAddVectorObserver() {
            // super(event_map);
        }

        @Override
        public Object master() {
            return FloatExpAddArray.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "FloatExpAddVectorObserver: " + _vars;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            // FloatEvent e = (FloatEvent) event;
            //
            // _sum.setMin(_sum.min() + e.mindiff());
            // _sum.setMax(_sum.max() + e.maxdiff());

            // Don't use delta-recalculation due to precision loss
            _sum.setMin(calc_min());
            _sum.setMax(calc_max());
        }
    } // ~ FloatExpAddVectorObserver
    static final private int[] event_map = { MIN, MIN, MAX, MAX, MIN | MAX | VALUE, VALUE,
    // REMOVE, REMOVE
    };
    private FloatExpArray _vars;

    private Observer _observer;

    private FloatVar _sum;

    public FloatExpAddArray(Constrainer constrainer, FloatExpArray vars) {
        super(constrainer, "");// exp.name()+"+"+value);
        _vars = vars;
        _observer = new FloatExpAddVectorObserver();

        FloatExp[] data = _vars.data();

        for (int i = 0; i < data.length; i++) {
            data[i].attachObserver(_observer);
        }

        String sum_name = "";

        if (constrainer().showInternalNames()) {
            StringBuffer s = new StringBuffer();
            s.append("(");
            for (int i = 0; i < data.length; i++) {
                if (i != 0) {
                    s.append("+");
                }
                s.append(data[i].name());
            }
            s.append(")");
            _name = s.toString();

            sum_name = "sum(" + _vars.name() + ")";
        }

        int trace = 0;
        double min = calc_min();
        double max = calc_max();
        _sum = constrainer().addFloatVarTraceInternal(min, max, sum_name, trace);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _sum.attachObserver(observer);
    }

    public double calc_max() {
        double max_sum = 0;

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            max_sum += vars[i].max();
        }
        return max_sum;
    }

    double calc_min() {
        double min_sum = 0;

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            min_sum += vars[i].min();
        }
        return min_sum;
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        if (!isLinear()) {
            throw new NonLinearExpression(this);
        }
        double cumSum = 0;
        for (int i = 0; i < _vars.size(); i++) {
            cumSum += _vars.get(i).calcCoeffs(map, factor);
        }
        return cumSum;
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _sum.detachObserver(observer);
    }

    @Override
    public boolean isLinear() {
        for (int i = 0; i < _vars.size(); i++) {
            if (!_vars.get(i).isLinear()) {
                return false;
            }
        }
        return true;
    }

    public double max() {
        return _sum.max();
    }

    public double min() {
        return _sum.min();
    }

    @Override
    public void name(String name) {
        super.name(name);
        _sum.name(name);
    }

    @Override
    public void onMaskChange() {
        // int mask = publisherMask();
        // FloatExp[] data =_vars.data();
        // for(int i=0; i < data.length; i++)
        // {
        // _observer.publish(mask,data[i]);
        // }
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _sum.reattachObserver(observer);
    }

    public void setMax(double max) throws Failure {

        if (max >= max()) {
            return;
        }

        // System.out.println("++++ Set max: " + max + " in " + this);

        double min_sum = min();

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            FloatExp vari = vars[i];
            double maxi = max - (min_sum - vari.min());
            if (maxi < vari.max()) {
                vari.setMax(maxi);
            }
        }
        // System.out.println("---- set max:" + max + " in " + this);
    }

    public void setMin(double min) throws Failure {

        if (min <= min()) {
            return;
        }

        // System.out.println("++++ Set min: " + min + " in " + this);

        double max_sum = max();

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            FloatExp vari = vars[i];
            double mini = min - (max_sum - vari.max());
            if (mini > vari.min()) {
                vari.setMin(mini);
            }
        }
        // System.out.println("---- set min:" + min + " in " + this);
    }

    public void setValue(double value) throws Failure {
        double sum_min = min();
        double sum_max = max();

        if (value < sum_min || value > sum_max) {
            _constrainer.fail("Float Add Array Set Value");
        }

        if (value == sum_min) {
            setMax(value);
            return;
        }
        if (value == sum_max) {
            setMin(value);
            return;
        }

        FloatExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            FloatExp vari = vars[i];
            double mini = vari.min();
            double maxi = vari.max();

            double new_min = value - (sum_max - maxi);
            if (new_min > mini) {
                vari.setMin(new_min);
            }

            double new_max = value - (sum_min - mini);
            if (new_max < maxi) {
                vari.setMax(new_max);
            }
        }

    }

    // public String toString()
    // {
    // return (_sum.toString() + " vars: {" + _vars + "}" );
    // }

} // ~FloatExpAddArray
