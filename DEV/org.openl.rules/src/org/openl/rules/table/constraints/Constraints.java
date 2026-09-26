package org.openl.rules.table.constraints;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.openl.util.CollectionUtils;

/**
 * @author Andrei Astrouski
 */
public class Constraints {

    private List<Constraint> items = new ArrayList<>();
    @Getter
    private String constraintsStr;

    public Constraints() {
    }

    public Constraints(List<Constraint> constraints) {
        setAll(constraints);
    }

    public Constraints(String constraintsStr) {
        setAll(constraintsStr);
    }

    public void setAll(String constraintsStr) {
        this.constraintsStr = constraintsStr;
        setAll(ConstraintsParser.parse(constraintsStr));
    }

    public void setAll(List<Constraint> constraints) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            this.items = new ArrayList<>(constraints);
        }
    }

    public List<Constraint> getAll() {
        return new ArrayList<>(items);
    }

    public void addAll(String constraintsStr) {
        addAll(ConstraintsParser.parse(constraintsStr));
    }

    public void addAll(List<Constraint> constraints) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            this.items.addAll(constraints);
        }
    }

    public void add(Constraint constraint) {
        items.add(constraint);
    }

    public Constraint get(int index) {
        return items.get(index);
    }

    public void remove(Constraint constraint) {
        items.remove(constraint);
    }

    public void remove(int index) {
        items.remove(index);
    }

    public int size() {
        return items.size();
    }
}
