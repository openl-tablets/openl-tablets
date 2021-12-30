package org.openl.rules.lang.xls.binding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;

public class CustomSpreadsheetResultOpenClassesKey {
    private final Set<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClasses;

    public CustomSpreadsheetResultOpenClassesKey(
            CustomSpreadsheetResultOpenClass... customSpreadsheetResultOpenClasses) {
        Objects.requireNonNull(customSpreadsheetResultOpenClasses, "customSpreadsheetResultOpenClass cannot be null");
        if (customSpreadsheetResultOpenClasses.length < 2) {
            throw new IllegalArgumentException();
        }
        this.customSpreadsheetResultOpenClasses = new HashSet<>(Arrays.asList(customSpreadsheetResultOpenClasses));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CustomSpreadsheetResultOpenClassesKey that = (CustomSpreadsheetResultOpenClassesKey) o;

        return Objects.equals(customSpreadsheetResultOpenClasses, that.customSpreadsheetResultOpenClasses);
    }

    @Override
    public int hashCode() {
        return customSpreadsheetResultOpenClasses.hashCode();
    }
}
