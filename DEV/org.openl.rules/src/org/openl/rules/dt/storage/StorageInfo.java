package org.openl.rules.dt.storage;

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;

public class StorageInfo {
    @Getter
    private int numberOfSpaces;
    @Getter
    private int numberOfFormulas;
    private int numberOfElses;

    @Getter(AccessLevel.PACKAGE)
    private final Map<Object, Integer> uniqueIndex = new HashMap<>();

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
