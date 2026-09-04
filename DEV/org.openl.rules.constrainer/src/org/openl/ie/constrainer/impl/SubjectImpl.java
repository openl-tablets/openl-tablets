package org.openl.ie.constrainer.impl;

import java.util.Collection;
import java.util.HashSet;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.constrainer.Undoable;
import org.openl.ie.tools.FastVector;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the Subject interface.
 */
public abstract class SubjectImpl extends UndoableOnceImpl implements Subject {
    /**
     * Undo Class for attached Observers.
     */
    static final class UndoAttachObserver extends UndoImpl {
        static final ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoAttachObserver();
            }

        };

        Observer _observer;

        static UndoAttachObserver getUndo(Subject subject, Observer observer) {
            var undo = (UndoAttachObserver) _factory.getElement();
            undo.undoable(subject);
            undo._observer = observer;
            return undo;
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoAttachObserver " + _observer;
        }

        @Override
        public void undo() {
            var subject = (Subject) undoable();
            subject.forcedDetachObserver(_observer);
        }

    } // ~UndoAttachObserver

    /**
     * Undo Class for detach Observers.
     */
    static final class UndoDetachObserver extends UndoImpl {

        static final ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoDetachObserver();
            }

        };

        private Observer _observer;

        static UndoDetachObserver getUndo(Subject subject, Observer observer) {
            var undo = (UndoDetachObserver) _factory.getElement();
            undo.undoable(subject);
            undo._observer = observer;
            return undo;
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoDetachObserver " + _observer;
        }

        @Override
        public void undo() {
            var subject = (Subject) undoable();
            subject.forcedAttachObserver(_observer);
        }

    } // ~UndoDetachObserver

    /**
     * Undo Class for Subject.
     */
    public static class UndoSubject extends UndoImpl {

        static final ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoSubject();
            }

        };

        private int _event_mask;

        static UndoSubject getUndo() {
            return (UndoSubject) _factory.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoSubject " + undoable() + ": mask=" + _event_mask;
        }

        @Override
        public void undo() {
            var subject = (Subject) undoable();
            subject.forcePublisherMask(_event_mask);
            super.undo();
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            var subject = (Subject) u;
            _event_mask = subject.publisherMask();
        }

    } // ~UndoSubject

    protected final FastVector _observers;

    protected boolean _in_process;

    protected int _publisher_mask;

    public SubjectImpl(Constrainer constrainer) {
        this(constrainer, "");
    }

    public SubjectImpl(Constrainer constrainer, String name) {
        super(constrainer, name);
        if (constrainer.showVariableNames()) {
            _name = name;
        }

        _observers = new FastVector(); // 10,5);

        _in_process = false;
    }

    public void addToPropagationQueue() {
        if (!_in_process) {
            constrainer().addToPropagationQueue(this);
            _in_process = true;
        }
    }

    @Override
    public Collection allDependents() {
        var dependendts = new HashSet();

        for (var i = 0; i < _observers.size(); ++i) {
            var obs = (Observer) _observers.elementAt(i);
            var master = obs.master();

            if (master == null) {
                continue;
            }

            if (dependendts.contains(master)) {
                continue;
            }

            dependendts.add(master);

            if (master instanceof Subject subject) {
                dependendts.addAll(subject.allDependents());
            }

        }

        return dependendts;
    }

    @Override
    public void attachObserver(Observer observer) {
        _observers.addElement(observer);
        publisherMask(_publisher_mask | observer.subscriberMask());
        constrainer().addUndo(UndoAttachObserver.getUndo(this, observer));
    }

    @Override
    public Undo createUndo() {
        return UndoSubject.getUndo();
    }

    @Override
    public void detachObserver(Observer observer) {

        _observers.removeElement(observer);
        constrainer().addUndo(UndoDetachObserver.getUndo(this, observer));
    }

    @Override
    public void forcedAttachObserver(Observer observer) {
        _observers.addElement(observer);
    }

    @Override
    public void forcedDetachObserver(Observer observer) {
        _observers.removeElement(observer);
    }

    @Override
    public void forcePublisherMask(int mask) {
        _publisher_mask = mask;
    }

    @Override
    public void inProcess(boolean flag) {
        _in_process = flag;
    }

    @Override
    final public void notifyObservers(EventOfInterest interest) throws Failure {
        var observers = _observers;
        _constrainer.incrementNumberOfNotifications();
        var size = observers.size();
        for (var i = 0; i < size; ++i) {
            var observer = (Observer) observers.elementAt(i);
            if (observer.interestedIn(interest)) {
                observer.update(this, interest);
            }
        }
        interest.free();
    }

    public void onMaskChange() {
    }

    @Override
    public abstract void propagate() throws Failure;

    @Override
    public void publish(int mask) {
        publisherMask(_publisher_mask | mask);
    }

    @Override
    public int publisherMask() {
        return _publisher_mask;
    }

    @Override
    public void publisherMask(int mask) {

        if (mask != _publisher_mask) {
            _publisher_mask = mask;
            onMaskChange();
            addUndo();
        }
    }

    @Override
    public void reattachObserver(Observer observer) {
        publisherMask(_publisher_mask | observer.subscriberMask());
    }

    @Override
    public void trace() {
        class ObserverTraceAll extends Observer {
            @Override
            public Object master() {
                return null;
            }

            @Override
            public int subscriberMask() {
                return EventOfInterest.ALL;
            }

            @Override
            public void update(Subject var, EventOfInterest interest) throws Failure {
                System.out.println("Trace " + interest + ": " + var);
            }

        } // ~ ObserverTraceAll

        attachObserver(new ObserverTraceAll());
    }

    @Override
    public void trace(int event_type) {
        @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
        class ObserverTrace extends Observer {
            private final int _event_type;

            @Override
            public Object master() {
                return null;
            }

            @Override
            public int subscriberMask() {
                return _event_type;
            }

            @Override
            public void update(Subject var, EventOfInterest interest) throws Failure {
                System.out.println("Trace " + interest + ": " + var);
            }

        } // ~ ObserverTrace

        attachObserver(new ObserverTrace(event_type));
    }

} // ~SubjectImpl
