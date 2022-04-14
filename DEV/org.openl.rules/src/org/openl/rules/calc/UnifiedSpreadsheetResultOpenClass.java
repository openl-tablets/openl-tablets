package org.openl.rules.calc;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;

public class UnifiedSpreadsheetResultOpenClass extends CustomSpreadsheetResultOpenClass {
    private static final int MAX_LENGTH_DISPLAY_NAME = 150;

    private final Set<CustomSpreadsheetResultOpenClass> unifiedOpenClasses = new HashSet<>();

    public UnifiedSpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super("UnifiedSpreadsheetResult", module, null, false, false);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void registerCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (customSpreadsheetResultOpenClass instanceof UnifiedSpreadsheetResultOpenClass) {
            UnifiedSpreadsheetResultOpenClass unifiedSpreadsheetResultOpenClass = (UnifiedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass;
            for (CustomSpreadsheetResultOpenClass o : unifiedSpreadsheetResultOpenClass.unifiedOpenClasses) {
                unifiedOpenClasses.add(o);
                o.addEventOnUpdateWithType(this::notifyChanges);
            }
        } else {
            unifiedOpenClasses.add(customSpreadsheetResultOpenClass);
            customSpreadsheetResultOpenClass.addEventOnUpdateWithType(this::notifyChanges);
        }
    }

    @Override
    public void addEventOnUpdateWithType(Consumer<CustomSpreadsheetResultOpenClass> c) {
        throw new IllegalStateException("Update is based on CustomSpreadsheetOpenClass");
    }

    public void notifyChanges(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        super.updateWithType(customSpreadsheetResultOpenClass);
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

    public CustomSpreadsheetResultOpenClass convertToModuleType(ModuleOpenClass module, boolean register) {
        if (getModule() != module) {
            if (register) {
                throw new IllegalStateException("Not supported for unified spreadsheet result type.");
            }
            CustomSpreadsheetResultOpenClass[] customSpreadsheetResultOpenClasses = getUnifiedTypes().stream()
                .map(((XlsModuleOpenClass) module)::toModuleType)
                .filter(e -> e instanceof CustomSpreadsheetResultOpenClass)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .toArray(CustomSpreadsheetResultOpenClass[]::new);
            CustomSpreadsheetResultOpenClass type = ((XlsModuleOpenClass) module)
                .buildOrGetUnifiedSpreadsheetResult(customSpreadsheetResultOpenClasses);
            type.setMetaInfo(getMetaInfo());
            return type;
        }
        return this;
    }

    public String getBeanClassName() {
        if (beanClassName == null) {
            synchronized (this) {
                if (beanClassName == null) {
                    String name = getUnifiedTypes().stream()
                        .map(CustomSpreadsheetResultOpenClass::getName)
                        .sorted()
                        .collect(Collectors.joining());
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    @Override
    public String getName() {
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
