package org.openl.rules.table.constraints;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.CollectionUtils;

/**
 * @author Andrei Astrouski
 */
public class Constraints {

    private List<Constraint> constraints = new ArrayList<>();
    private String constraintsStr;

    public Constraints() {
    }

    public Constraints(List<Constraint> constraints) {
        setAll(constraints);
    }

    public Constraints(String constraintsStr) {
        setAll(constraintsStr);
    }

    public String getConstraintsStr() {
        return constraintsStr;
    }

    public void setAll(String constraintsStr) {
        this.constraintsStr = constraintsStr;
        setAll(ConstraintsParser.parse(constraintsStr));
    }

    public void setAll(List<Constraint> constraints) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            this.constraints = new ArrayList<>(constraints);
        }
    }

    public List<Constraint> getAll() {
        return new ArrayList<>(constraints);
    }

    public void addAll(String constraintsStr) {
        addAll(ConstraintsParser.parse(constraintsStr));
    }

    public void addAll(List<Constraint> constraints) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            this.constraints.addAll(constraints);
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
