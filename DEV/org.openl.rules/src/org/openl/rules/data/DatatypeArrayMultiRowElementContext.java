package org.openl.rules.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class DatatypeArrayMultiRowElementContext {
    private Map<String, Map<Integer, Pair<Integer, Object>>> arrayIndexes = new HashMap<>();

    private int row;

    private boolean rowValueIsTheSameAsPrevious;

    public int getIndex(String array, Object target) {
        Map<Integer, Pair<Integer, Object>> a = arrayIndexes.get(array);
        if (a == null) {
            a = new HashMap<>();
            arrayIndexes.put(array, a);
        }
        Pair<Integer, Object> index = a.get(getRow());
        if (index != null) {
            return index.getLeft();
        } else {
            if (getRow() > 0) {
                Pair<Integer, Object> prevIndex = a.get(getRow() - 1);
                if (prevIndex != null && prevIndex.getRight() == target) {
                    if (isRowValueIsTheSameAsPrevious()) {
                        index = new ImmutablePair<>(prevIndex.getLeft(), target);
                    } else {
                        index = new ImmutablePair<>(prevIndex.getLeft() + 1, target);
                    }
                } else {
                    index = new ImmutablePair<>(0, target);
                }
            } else {
                index = new ImmutablePair<>(0, target);
            }
        }
        a.put(getRow(), index);
        return index.getLeft();
    }

    public void setRowValueIsTheSameAsPrevious(boolean rowValueIsTheSameAsPrevious) {
        this.rowValueIsTheSameAsPrevious = rowValueIsTheSameAsPrevious;
    }

    public boolean isRowValueIsTheSameAsPrevious() {
        return rowValueIsTheSameAsPrevious;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getRow() {
        return row;
    }
}
