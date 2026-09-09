package org.openl.rules.webstudio.web.test;

import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IOpenClass;

public final class Utils {
    private Utils() {
    }

    public static boolean isCollection(IOpenClass openClass) {
        return openClass.getAggregateInfo() != null && openClass.getAggregateInfo().isAggregate(openClass);
    }

    public static String displayNameForCollection(IOpenClass collectionType, boolean isEmpty) {
        var builder = new StringBuilder();
        if (isEmpty) {
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(collectionType.getComponentClass().getDisplayName(INamedThing.SHORT));
        return builder.toString();
    }

    public static IDataBase getDb(ProjectModel model, boolean currentOpenedModule) {
        if (model == null) {
            return null;
        }
        CompiledOpenClass compiledOpenClass = currentOpenedModule ? model.getOpenedModuleCompiledOpenClass()
                : model.getCompiledOpenClass();
        var moduleClass = compiledOpenClass.getOpenClassWithErrors();
        if (moduleClass instanceof XlsModuleOpenClass class1) {
            return class1.getDataBase();
        }

        return null;
    }
}
