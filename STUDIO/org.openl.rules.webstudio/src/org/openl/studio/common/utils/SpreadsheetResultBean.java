package org.openl.studio.common.utils;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.util.OpenClassUtils;

/**
 * A spreadsheet result value seen the way OpenL Rule Services publishes it.
 *
 * <p>A spreadsheet result carries no properties of its own to read or write, so it travels as the bean class OpenL
 * generates for the spreadsheet. Writing a value produces that bean, and reading one back turns it into a
 * spreadsheet result again.
 *
 * @param type      the spreadsheet the value belongs to
 * @param beanClass the class the value travels as, keeping the array dimensions of the declared type
 */
public record SpreadsheetResultBean(CustomSpreadsheetResultOpenClass type, Class<?> beanClass) {

    /**
     * Reads a declared type as a spreadsheet result, looking through array dimensions.
     *
     * @param declaredType declared type of a value
     * @return the published view of the type, or {@code null} when the type is not a spreadsheet result
     */
    @Nullable
    public static SpreadsheetResultBean of(IOpenClass declaredType) {
        var spreadsheetResultType = typeOf(OpenClassUtils.getRootComponentClass(declaredType));
        if (spreadsheetResultType == null) {
            return null;
        }
        var beanClass = spreadsheetResultType.getBeanClass();
        for (var dimension = OpenClassUtils.getDimension(declaredType); dimension > 0; dimension--) {
            beanClass = beanClass.arrayType();
        }
        return new SpreadsheetResultBean(spreadsheetResultType, beanClass);
    }

    /**
     * Every spreadsheet result bean class of the module, keyed by the class, so a bean read from JSON can be
     * matched back to the spreadsheet it describes.
     */
    public static Map<Class<?>, CustomSpreadsheetResultOpenClass> beanClassesOf(XlsModuleOpenClass module) {
        var beanClasses = new HashMap<Class<?>, CustomSpreadsheetResultOpenClass>();
        for (IOpenClass type : module.getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass spreadsheetResultType) {
                beanClasses.put(spreadsheetResultType.getBeanClass(), spreadsheetResultType);
            }
        }
        module.getCombinedSpreadsheetResultOpenClasses()
                .forEach(combinedType -> beanClasses.put(combinedType.getBeanClass(), combinedType));
        var anyType = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes();
        if (anyType != null) {
            var combinedType = anyType.toCustomSpreadsheetResultOpenClass();
            beanClasses.put(combinedType.getBeanClass(), combinedType);
        }
        return beanClasses;
    }

    @Nullable
    private static CustomSpreadsheetResultOpenClass typeOf(IOpenClass baseType) {
        return switch (baseType) {
            case CustomSpreadsheetResultOpenClass spreadsheetResultType -> spreadsheetResultType;
            // A value declared as the bare SpreadsheetResult stands for any spreadsheet of the module, which the
            // module publishes as a single combined type.
            case SpreadsheetResultOpenClass anyType when anyType.getModule() != null ->
                    anyType.toCustomSpreadsheetResultOpenClass();
            default -> null;
        };
    }
}
