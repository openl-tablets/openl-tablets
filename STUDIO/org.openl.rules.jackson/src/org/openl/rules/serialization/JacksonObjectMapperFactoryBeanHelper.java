package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public final class JacksonObjectMapperFactoryBeanHelper {
    private JacksonObjectMapperFactoryBeanHelper() {
    }

    public static Set<Class<?>> extractSpreadsheetResultBeanClasses(XlsModuleOpenClass xlsModuleOpenClass,
            Class<?> serviceClass) {
        boolean found = false;
        Set<Class<?>> ret = new HashSet<>();
        for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) type;
                CustomSpreadsheetResultOpenClass csrt = (CustomSpreadsheetResultOpenClass) xlsModuleOpenClass
                    .findType(customSpreadsheetResultOpenClass.getName());
                Class<?> beanClass = csrt.getBeanClass();
                ret.add(beanClass);
                if (!found) {
                    for (Method method : serviceClass.getMethods()) {
                        if (!found && ClassUtils.isAssignable(beanClass, method.getReturnType())) {
                            found = true;
                        }
                    }
                }
            }
        }

        Class<?> spreadsheetResultBeanClass = xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
            .toCustomSpreadsheetResultOpenClass()
            .getBeanClass();
        ret.add(spreadsheetResultBeanClass);
        if (!found) {
            for (Method method : serviceClass.getMethods()) {
                if (!found && ClassUtils.isAssignable(spreadsheetResultBeanClass, method.getReturnType())) {
                    found = true;
                }
            }
        }
        return found ? ret : Collections.emptySet();
    }
}
