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

public class UnifiedSpreadsheetResultOpenClass extends CustomSpreadsheetResultOpenClass {
    private static final int MAX_LENGTH_DISPLAY_NAME = 150;

    private final Set<CustomSpreadsheetResultOpenClass> unifiedOpenClasses = new HashSet<>();

    private final boolean anonymous;

    public UnifiedSpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super("UnifiedSpreadsheetResult", module, null, false);
        this.anonymous = true;
    }

    public UnifiedSpreadsheetResultOpenClass(String name, XlsModuleOpenClass module) {
        super(name, module, null, true);
        this.anonymous = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void registerCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (customSpreadsheetResultOpenClass instanceof UnifiedSpreadsheetResultOpenClass) {
            UnifiedSpreadsheetResultOpenClass unifiedSpreadsheetResultOpenClass = (UnifiedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass;
            if (unifiedSpreadsheetResultOpenClass.anonymous) {
                unifiedOpenClasses
                    .addAll(((UnifiedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass).unifiedOpenClasses);
                return;
            }
        }
        unifiedOpenClasses.add(customSpreadsheetResultOpenClass);
    }

    public Collection<CustomSpreadsheetResultOpenClass> getUnifiedTypes() {
        return Collections.unmodifiableCollection(unifiedOpenClasses);
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
        if (ioc instanceof UnifiedSpreadsheetResultOpenClass) {
            UnifiedSpreadsheetResultOpenClass unifiedSpreadsheetResultOpenClass = (UnifiedSpreadsheetResultOpenClass) ioc;
            return getUnifiedTypes().stream()
                .map(IOpenClass::getName)
                .collect(Collectors.toSet())
                .containsAll(unifiedSpreadsheetResultOpenClass.getUnifiedTypes()
                    .stream()
                    .map(IOpenClass::getName)
                    .collect(Collectors.toSet()));
        }
        if (ioc instanceof CustomSpreadsheetResultOpenClass) {
            return getUnifiedTypes().stream().map(IOpenClass::getName).anyMatch(e -> e.equals(ioc.getName()));
        }
        return false;
    }

    @Override
    public String getName() {
        if (anonymous) {
            StringBuilder sb = new StringBuilder();
            List<CustomSpreadsheetResultOpenClass> types = getUnifiedTypes().stream()
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
            return name + "&...(" + (getUnifiedTypes().size() - c) + ") more";
        }
        return name;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
