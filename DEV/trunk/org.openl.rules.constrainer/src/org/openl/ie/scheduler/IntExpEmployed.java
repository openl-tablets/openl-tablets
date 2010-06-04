package org.openl.ie.scheduler;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.impl.IntEvent;
import org.openl.ie.constrainer.impl.IntExpImpl;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

class IntExpEmployed extends IntExpImpl {

    class EmployedObserver extends Observer {

        @Override
        public Object master() {
            return IntExpEmployed.this;
        }

        @Override
        public int subscriberMask() {
            return EventOfInterest.VALUE;
        }

        @Override
        public void update(Subject var, EventOfInterest event) throws Failure {
            _value = 0;
            for (int i = 0; i < _resource.duration(); i++) {
                if (_resource.caps().get(i).bound()) {
                    _value++;
                }
            }

            _var.setMin(_value);

            IntEvent e = (IntEvent) event;

            IntEventEmployed ev = IntEventEmployed.getEvent(e);

            // notifyObservers(ev);
        }
    }
    static final class IntEventEmployed extends IntEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new IntEventEmployed();
            }

        };

        IntEvent _event;

        int _type = 0;

        static IntEventEmployed getEvent(IntEvent event) {
            IntEventEmployed ev = (IntEventEmployed) _factory.getElement();
            ev.init(event);
            return ev;
        }

        void init(IntEvent event) {
            _event = event;
            _type |= MIN;
            _type |= MAX;
            _type |= VALUE;
        }

        @Override
        public int max() {
            return _event.max();
        }

        @Override
        public int min() {
            return _event.min();
        }

        @Override
        public String name() {
            return "IntEventEmployed";
        }

        @Override
        public int numberOfRemoves() {
            return 0;
        }

        @Override
        public int oldmax() {
            return _event.oldmax();
        }

        @Override
        public int oldmin() {
            return _event.oldmin();
        }

        @Override
        public int removed(int i) {
            return 0;
        }

        @Override
        public int type() {
            return _type;
        }

    }
    private int _value;
    private IntVar _var;

    private Resource _resource;

    private Observer _observer;

    public IntExpEmployed(Resource res) {
        super(res.constrainer());
        _resource = res;
        _observer = new EmployedObserver();
        _var = constrainer().addIntVar(0, _resource.duration());
        for (int i = 0; i < _resource.duration(); i++) {
            _resource.caps().get(i).attachObserver(_observer);
        }
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _var.attachObserver(observer);
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _var.detachObserver(observer);
    }

    public int max() {
        return _var.max();
    }

    public int min() {
        return _var.min();
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _var.reattachObserver(observer);
    }

    public void setMax(int m) throws Failure {
        _var.setMax(m);
    }

    // ////////////////////////////////////////////////////

    public void setMin(int m) throws Failure {
        _var.setMin(m);
    }
}
