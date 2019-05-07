package org.openl.rules.dt.storage;

import java.util.HashMap;
import java.util.Map;

public class StorageInfo {
    private int numberOfSpaces = 0;
    private int numberOfFormulas = 0;
    private int numberOfElses = 0;

    private Map<Object, Integer> uniqueIndex = new HashMap<>();

    public int getNumberOfSpaces() {
        return numberOfSpaces;
    }

    public int getNumberOfFormulas() {
        return numberOfFormulas;
    }

    Map<Object, Integer> getUniqueIndex() {
        return uniqueIndex;
    }

    int getTotalNumberOfUniqueValues() {
        return uniqueIndex.size() + numberOfFormulas + (numberOfSpaces > 0 ? 1 : 0) + (numberOfElses > 0 ? 1 : 0);
    }

    void addSpaceIndex() {
        numberOfSpaces++;
    }

    void addElseIndex() {
        numberOfElses++;
    }

    void addFormulaIndex() {
        numberOfFormulas++;
    }
}
