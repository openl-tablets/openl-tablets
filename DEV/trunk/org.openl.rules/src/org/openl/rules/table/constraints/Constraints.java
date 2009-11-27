package org.openl.rules.table.constraints;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Andrei Astrouski
 */
public class Constraints {

    private List<Constraint> constraints = new ArrayList<Constraint>();

    public Constraints() {
    }

    public Constraints(List<Constraint> constraints) {
        setAll(constraints);
    }

    public Constraints(String constraintsStr) {
        setAll(constraintsStr);
    }

    public void setAll(String constraintsStr) {
        List<Constraint> constraints = ConstraintsParser.parse(constraintsStr);
        setAll(constraints);
    }

    public void setAll(List<Constraint> constraints) {
        if (!constraints.isEmpty()) {
            this.constraints = constraints;
        }
    }

    public List<Constraint> getAll() {
        return constraints;
    }

    public void addAll(String constraintsStr) {
        List<Constraint> constraints = ConstraintsParser.parse(constraintsStr);
        if (!constraints.isEmpty()) {
            constraints.addAll(constraints);
        }
    }

    public void add(Constraint constraint) {
        constraints.add(constraint);
    }

    public Constraint get(int index) {
        return constraints.get(index);
    }

    public void remove(Constraint constraint) {
        constraints.remove(constraint);
    }

    public void remove(int index) {
        constraints.remove(index);
    }

    public int size() {
        return constraints.size();
    }

}
