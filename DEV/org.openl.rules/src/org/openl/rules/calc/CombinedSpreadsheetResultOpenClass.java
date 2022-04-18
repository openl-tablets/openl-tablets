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

/**
 * This class is designed to implement a functionality of combination custom spreadsheet result types.
 *
 * @author Marat Kamalov
 *
 */
public class CombinedSpreadsheetResultOpenClass extends CustomSpreadsheetResultOpenClass {
    private static final int MAX_LENGTH_DISPLAY_NAME = 150;
    public static final int MAX_BEANCLASSNAME_LENGTH = 200;

    private final Set<CustomSpreadsheetResultOpenClass> combinedOpenClasses = new HashSet<>();

    public CombinedSpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super("CombinedSpreadsheetResult", module, null, true, false);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void registerCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (customSpreadsheetResultOpenClass instanceof CombinedSpreadsheetResultOpenClass) {
            CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass = (CombinedSpreadsheetResultOpenClass) customSpreadsheetResultOpenClass;
            for (CustomSpreadsheetResultOpenClass o : combinedSpreadsheetResultOpenClass.combinedOpenClasses) {
                combinedOpenClasses.add(o);
                o.addEventOnUpdateWithType(this::notifyChanges);
            }
        } else {
            combinedOpenClasses.add(customSpreadsheetResultOpenClass);
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

    /**
     * Convert this type to a type belongs to provided module.
     * 
     * @param module
     * @return converted type
     */
    public CustomSpreadsheetResultOpenClass convertToModuleType(ModuleOpenClass module, boolean register) {
        if (getModule() != module) {
            if (register) {
                throw new IllegalStateException("Not supported for combined spreadsheet result type.");
            }
            CustomSpreadsheetResultOpenClass[] customSpreadsheetResultOpenClasses = getCombinedTypes().stream()
                .map(((XlsModuleOpenClass) module)::toModuleType)
                .filter(e -> e instanceof CustomSpreadsheetResultOpenClass)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .toArray(CustomSpreadsheetResultOpenClass[]::new);
            CustomSpreadsheetResultOpenClass type = ((XlsModuleOpenClass) module)
                .buildOrGetCombinedSpreadsheetResult(customSpreadsheetResultOpenClasses);
            type.setMetaInfo(getMetaInfo());
            return type;
        }
        return this;
    }

    @Override
    public String getBeanClassName() {
        if (beanClassName == null) {
            synchronized (this) {
                if (beanClassName == null) {
                    String name = getCombinedTypes().stream()
                        .map(CustomSpreadsheetResultOpenClass::getName)
                        .map(this::spreadsheetResultNameToBeanName)
                        .sorted()
                        .collect(Collectors.joining());
                    if (name.length() > MAX_BEANCLASSNAME_LENGTH) {
                        name = "CombinedType" + getModule().getCombinedSpreadsheetResultOpenClassesCounter()
                            .incrementAndGet();
                    }
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    @Override
    public String getName() {
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
