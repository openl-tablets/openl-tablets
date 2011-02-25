package org.openl.rules.liveexcel;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

import com.exigen.le.LE_Value;
import com.exigen.le.smodel.Function.FunctionArgument;

public class TypeUtils {

    public static IOpenClass getOpenClass(FunctionArgument functionArg, RulesModuleBindingContext bindingContext) {
        // FIXME typeName sometimes is empty
        String typeName = functionArg.getTypeName() != null ? functionArg.getTypeName() : functionArg.getDescription();
        if (typeName != null) {
            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
            if (type != null) {
                return type;
            }
        }
        return getSimpleType(typeName);
    }

    public static IOpenClass getOpenClass(String typeName, RulesModuleBindingContext bindingContext) {
        if (typeName != null) {
            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
            if (type != null) {
                return type;
            }
        }
        return getSimpleType(typeName);
    }

    public static IOpenClass getSimpleType(String typeName) {
        if (LE_Value.TypeString.NUMERIC.equalsIgnoreCase(typeName)) {
            return JavaOpenClass.DOUBLE;
        } else if (LE_Value.TypeString.STRING.equalsIgnoreCase(typeName)) {
            return JavaOpenClass.STRING;
        } else if (LE_Value.TypeString.BOOLEAN.equalsIgnoreCase(typeName)) {
            return JavaOpenClass.BOOLEAN;
        } else if (LE_Value.TypeString.DATE.equalsIgnoreCase(typeName)) {
            return JavaOpenClass.getOpenClass(Date.class);
        }
        return JavaOpenClass.OBJECT;
    }

    public static String convertName(String name) {
        if (name != null) {
            String[] parts = name.split(" ");
            parts[0] = StringUtils.uncapitalize(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                parts[i] = StringUtils.capitalize(parts[i]);
            }
            return StringUtils.join(parts);
        }
        return null;
    }

}
