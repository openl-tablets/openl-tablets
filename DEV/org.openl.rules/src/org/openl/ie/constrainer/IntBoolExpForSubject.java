package org.openl.ie.constrainer;

/**
 * A boolean expression that tells whether a statement about other expressions, the subject, is true.
 * <p>
 * The expression becomes true or false as soon as the subject is known to be true or false. Conversely, when a
 * constraint makes the expression true or false, the expression makes the subject true or false.
 */
abstract class IntBoolExpForSubject extends IntBoolVar {

    private static final int UNKNOWN = -1;

    // 1 when the subject is true, 0 when it is false.
    private int subject;

    IntBoolExpForSubject(Constrainer constrainer) {
        super(constrainer);
    }

    /**
     * Returns whether the subject is true. While the subject is unknown, it is neither true nor false.
     */
    abstract boolean isSubjectTrue();

    /**
     * Returns whether the subject is false. While the subject is unknown, it is neither true nor false.
     */
    abstract boolean isSubjectFalse();

    /**
     * Makes the subject true.
     *
     * @throws Failure if the subject is false
     */
    abstract void setSubjectTrue() throws Failure;

    /**
     * Makes the subject false.
     *
     * @throws Failure if the subject is true
     */
    abstract void setSubjectFalse() throws Failure;

    /**
     * Sets the initial domain from the subject. A subclass calls it at the end of its constructor.
     */
    final void initDomain() {
        if (isSubjectTrue()) {
            min = 1;
        } else if (isSubjectFalse()) {
            max = 0;
        }
        subject = min == max ? min : UNKNOWN;
    }

    /**
     * Binds the expression once the subject is known after its change. While the subject is unknown, makes the subject
     * follow the expression.
     */
    final void update() throws Failure {
        if (subject != UNKNOWN) {
            return;
        }
        if (isSubjectFalse()) {
            setSubject(0);
        } else if (isSubjectTrue()) {
            setSubject(1);
        }
        if (subject != UNKNOWN) {
            setValue(subject);
        } else if (isTrue()) {
            setSubjectTrue();
        } else if (isFalse()) {
            setSubjectFalse();
        }
    }

    private void setSubject(int value) {
        constrainer.addUndo(() -> subject = UNKNOWN);
        subject = value;
    }

    /**
     * Raises the minimum of the domain without changing the subject.
     */
    final void setDomainMin(int value) throws Failure {
        super.setMin(value);
    }

    /**
     * Lowers the maximum of the domain without changing the subject.
     */
    final void setDomainMax(int value) throws Failure {
        super.setMax(value);
    }

    @Override
    void setMin(int value) throws Failure {
        if (value <= min) {
            return;
        }
        super.setMin(value);
        setSubjectTrue();
    }

    @Override
    void setMax(int value) throws Failure {
        if (value >= max) {
            return;
        }
        super.setMax(value);
        setSubjectFalse();
    }
}
