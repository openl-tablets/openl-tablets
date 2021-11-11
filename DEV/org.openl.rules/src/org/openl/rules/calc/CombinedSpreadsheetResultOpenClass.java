package org.openl.rules.calc;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;

public class CombinedSpreadsheetResultOpenClass extends CustomSpreadsheetResultOpenClass {
    private static final int MAX_LENGTH_DISPLAY_NAME = 150;

    private final Set<CustomSpreadsheetResultOpenClass> combinedOpenClasses = new HashSet<>();

    private final boolean anonymous;

    public CombinedSpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super("CombinedSpreadsheetResult", module, null, false);
        this.anonymous = true;
    }

    public CombinedSpreadsheetResultOpenClass(String name, XlsModuleOpenClass module) {
        super(name, module, null, true);
        this.anonymous = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void registerCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (customSpreadsheetResultOpenClass instanceof CombinedSpreadsheetResultOpenClass) {
            CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass = (CombinedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass;
            if (combinedSpreadsheetResultOpenClass.anonymous) {
                combinedOpenClasses.addAll(
                    ((CombinedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass).combinedOpenClasses);
                return;
            }
        }
        combinedOpenClasses.add(customSpreadsheetResultOpenClass);
    }

    public Collection<CustomSpreadsheetResultOpenClass> getCombinedTypes() {
        return Collections.unmodifiableCollection(combinedOpenClasses);
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (openClass instanceof CustomSpreadsheetResultOpenClass) {
            registerCustomSpreadsheetResultOpenClass((CustomSpreadsheetResultOpenClass) openClass);
        }
        super.updateWithType(openClass);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof CombinedSpreadsheetResultOpenClass) {
            CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass = (CombinedSpreadsheetResultOpenClass) ioc;
            return getCombinedTypes().stream()
                .map(IOpenClass::getName)
                .collect(Collectors.toSet())
                .containsAll(combinedSpreadsheetResultOpenClass.getCombinedTypes()
                    .stream()
                    .map(IOpenClass::getName)
                    .collect(Collectors.toSet()));
        }
        if (ioc instanceof CustomSpreadsheetResultOpenClass) {
            return getCombinedTypes().stream().map(IOpenClass::getName).anyMatch(e -> e.equals(ioc.getName()));
        }
        return false;
    }

    @Override
    public String getName() {
        if (anonymous) {
            StringBuilder sb = new StringBuilder();
            List<CustomSpreadsheetResultOpenClass> types = getCombinedTypes().stream()
                .sorted(Comparator.comparing(CustomSpreadsheetResultOpenClass::getName))
                .collect(Collectors.toList());
            for (CustomSpreadsheetResultOpenClass c : types) {
                if (sb.length() > 0) {
                    sb.append(" & ");
                }
                sb.append(Spreadsheet.SPREADSHEETRESULT_SHORT_TYPE_PREFIX)
                    .append(c.getName().substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()));
            }
            return sb.toString();
        }
        return super.getName();
    }

    @Override
    public String getDisplayName(int mode) {
        String name = getName();
        boolean f = false;
        long c = 0;
        if (name.length() > MAX_LENGTH_DISPLAY_NAME) {
            name = name.substring(0, MAX_LENGTH_DISPLAY_NAME);
            name = name.substring(0, name.lastIndexOf("&"));
            f = true;
            c = name.chars().filter(ch -> ch == '&').count() + 1;
        }
        if (f) {
            return name + "&...(" + (getCombinedTypes().size() - c) + ") more";
        }
        return name;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
