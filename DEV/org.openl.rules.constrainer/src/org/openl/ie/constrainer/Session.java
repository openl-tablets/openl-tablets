package org.openl.ie.constrainer;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class Session {
    Constrainer _c = null;
    boolean restored = false;
    IntVar[] _intVars = null;
    FloatVar[] _floatVars = null;
    Goal _mainGoal = null;
    ChoicePointLabel _label = null;

    public Session(Constrainer c, IntVar[] intVarsOfInterest, FloatVar[] floatVarsOfInterest, Goal mainGoal) {
        if (c == null) {
            throw new IllegalArgumentException(
                    "Can't create session object: reference to Constrainer object equals \"null\"");
        }
        _c = c;
        _intVars = intVarsOfInterest;
        _floatVars = floatVarsOfInterest;
        _mainGoal = mainGoal;
    }

    public Session(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
        restore(in);
        restored = true;
    }

    public boolean execute(boolean restore_flag) {
        if (!restored) {
            return _c.execute(_mainGoal, restore_flag);
        }
        if (_mainGoal != null) {
            return _mainGoal.toContinue(_label, restore_flag);
        }
        return _c.toContinue(_label, restore_flag);
    }

    public Constrainer getConstrainer() {
        return _c;
    }

    public FloatVar[] getFloatVarsOfInterest() {
        return _floatVars;
    }

    public IntVar[] getIntVarsOfInterest() {
        return _intVars;
    }

    public Goal getMainGoal() {
        return _mainGoal;
    }

    public void restore(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
        _c = (Constrainer) in.readObject();
        _c.out(System.out);
        _mainGoal = (Goal) in.readObject();
        int[] indices = (int[]) in.readObject();
        _intVars = _c.getIntVars(indices);
        indices = (int[]) in.readObject();
        _floatVars = _c.getFloatVars(indices);
        _label = (ChoicePointLabel) in.readObject();
    }

    public void setChoicePointLabel(ChoicePointLabel label) {
        this._label = label;
    }

    public void setConstrainer(Constrainer C) {
        _c = C;
    }

    public void setFloatVarsOfInterest(FloatVar[] array) {
        _floatVars = array;
    }

    public void setIntVarsOfInterest(IntVar[] array) {
        _intVars = array;
    }

    public void setMainGoal(Goal goal) {
        _mainGoal = goal;
    }

    public void store(java.io.ObjectOutputStream sout) throws java.io.IOException {
        if (_label == null) {
            _label = _c.currentChoicePointLabel();
        }
        sout.writeObject(_c);
        sout.writeObject(_mainGoal);
        sout.writeObject(_c.findAppropriate(_intVars));
        sout.writeObject(_c.findAppropriate(_floatVars));
        sout.writeObject(_label);
    }

}