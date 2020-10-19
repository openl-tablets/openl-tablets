package org.openl.rules.project.validation.openapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

final class SpreadsheetMethodResolver {
    private final Context context;
    private Map<Class<?>, IOpenMethod> cache;

    public SpreadsheetMethodResolver(Context context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    private void initialize() {
        cache = new HashMap<>();
        for (IOpenMethod method : context.getOpenClass().getMethods()) {
            if (method.getType() instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) method
                    .getType();
                cache.put(customSpreadsheetResultOpenClass.getBeanClass(), method);
            }
        }
    }

    public IOpenMethod resolve(IOpenClass openClass) {
        if (cache == null) {
            initialize();
        }
        return cache.get(openClass.getInstanceClass());
    }

    public String resolveStepName(IOpenClass openClass, IOpenField beanField) {
        IOpenMethod method = resolve(openClass);
        if (method != null) {
            return resolveStepName(method, beanField);
        }
        return null;
    }

    private static String resolveStepName(Spreadsheet spreadsheet, IOpenField beanField) {
        IOpenField openFieldInSpr = findSpreadsheetOpenField(spreadsheet, beanField);
        return openFieldInSpr != null ? openFieldInSpr.getName() : null;
    }

    public static IOpenField findSpreadsheetOpenField(Spreadsheet spreadsheet, IOpenField beanField) {
        Map<String, List<IOpenField>> beanFieldsMap = ((CustomSpreadsheetResultOpenClass) spreadsheet.getType())
            .getBeanFieldsMap();
        List<IOpenField> sprFields = beanFieldsMap.get(beanField.getName());
        IOpenField openFieldInSpr = null;
        for (IOpenField f : sprFields) {
            IOpenField g = spreadsheet.getSpreadsheetType().getField(f.getName());
            if (openFieldInSpr == null || g != null && openFieldInSpr.getName().length() > g.getName().length()) {
                openFieldInSpr = g;
            }
        }
        return openFieldInSpr;
    }

    private static String resolveStepName(IOpenMethod method, IOpenField beanField) {
        if (!(method.getType() instanceof CustomSpreadsheetResultOpenClass)) {
            throw new IllegalStateException("Expected custom spreadsheet result");
        }
        if (method instanceof OpenMethodDispatcher) {
            OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
            for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                if (m instanceof Spreadsheet && m.getType() instanceof CustomSpreadsheetResultOpenClass) {
                    Spreadsheet spreadsheet = (Spreadsheet) m;
                    return resolveStepName(spreadsheet, beanField);
                }
            }
        } else if (method instanceof Spreadsheet) {
            Spreadsheet spreadsheet = (Spreadsheet) method;
            return resolveStepName(spreadsheet, beanField);
        }
        return null;
    }
}
