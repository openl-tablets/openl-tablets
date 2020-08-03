package org.open.rules.project.validation.openapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.types.IOpenClass;
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
}
