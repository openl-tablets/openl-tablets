package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;

/**
 * An implementation of the expression: <code>sum(IntExpArray)</code>.
 */
public final class IntExpAddArray extends IntExpImpl {
    class ExpAddVectorObserver extends Observer {

        ExpAddVectorObserver() {
            // super(event_map);
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
            IntEvent e = (IntEvent) event;

            // System.out.println("Event:" + e);

            _sum.setMin(_sum.min() + e.mindiff());
            _sum.setMax(_sum.max() + e.maxdiff());

        }

    } // ~ ExpAddVectorObserver

    static final private int[] event_map = { MIN, MIN, MAX, MAX, MIN | MAX | VALUE, VALUE,
            // REMOVE, REMOVE
    };
    private IntExpArray _vars;

    private Observer _observer;

    private IntVar _sum;

    public IntExpAddArray(Constrainer constrainer, IntExpArray vars) {
        super(constrainer);
        int size = vars.size();
        _vars = vars;
        _observer = new ExpAddVectorObserver();

        IntExp[] data = _vars.data();

        for (int i = 0; i < data.length; i++) {
            data[i].attachObserver(_observer);
        }

        String sum_name = "";

        if (constrainer().showInternalNames()) {
            StringBuilder s = new StringBuilder();
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
        _sum = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), sum_name, IntVar.DOMAIN_PLAIN, trace);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _sum.attachObserver(observer);
    }

    public int calc_max() {
        int max_sum = 0;

        IntExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            max_sum += vars[i].max();
        }
        return max_sum;
    }

    int calc_min() {
        int min_sum = 0;

        IntExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            min_sum += vars[i].min();
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
        for (int i = 0; i < _vars.size(); i++) {
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
        // int mask = publisherMask();
        // IntExp[] data =_vars.data();
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

    @Override
    public void removeValue(int value) throws Failure {
        int Max = max();
        if (value > Max) {
            return;
        }
        int Min = min();
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

        // System.out.println("++++ Set max: " + max + " in " + this);

        int min_sum = min();

        IntExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            IntExp vari = vars[i];
            int maxi = max - (min_sum - vari.min());
            if (maxi < vari.max()) {
                vari.setMax(maxi);
            }
        }
        // System.out.println("---- set max:" + max + " in " + this);
    }

    @Override
    public void setMin(int min) throws Failure {

        if (min <= min()) {
            return;
        }

        // System.out.println("++++ Set min: " + min + " in " + this);

        int max_sum = max();

        IntExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            IntExp vari = vars[i];
            int mini = min - (max_sum - vari.max());
            if (mini > vari.min()) {
                vari.setMin(mini);
            }
        }
        // System.out.println("---- set min:" + min + " in " + this);
    }

    @Override
    public void setValue(int value) throws Failure {
        int sum_min = min();
        int sum_max = max();

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

        // System.out.println("++++ Set value: " + value + " in " + this);

        IntExp[] vars = _vars.data();

        for (int i = 0; i < vars.length; i++) {
            IntExp vari = vars[i];
            int mini = vari.min();
            int maxi = vari.max();

            int new_min = value - (sum_max - maxi);
            if (new_min > mini) {
                vari.setMin(new_min);
            }

            int new_max = value - (sum_min - mini);
            if (new_max < maxi) {
                vari.setMax(new_max);
            }
        }

        // System.out.println("---- set value: " + value + " in " + this);
    }

    @Override
    public int size() {
        return max() - min() + 1;
    }

} // ~IntExpAddArray
