package org.openl.rules.liveexcel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.BindHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.SimpleBeanByteCodeGenerator;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass.OpenFieldsConstructor;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DatatypeOpenField;

import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Type;

public class LiveExcellDatatypeBinder {
    private List<Type> types;

    public LiveExcellDatatypeBinder(List<Type> types) {
        this.types = types;
    }

    public void preBind(XlsModuleOpenClass module, RulesModuleBindingContext bindingContext) {
        for (Type type : types) {
            try {
                // TODO package
                bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE,
                        new DatatypeOpenClass(module.getSchema(), type.getDeclaredName(), "org.openl.generated.beans"));
            } catch (Exception e) {
                BindHelper.processError(String.format("Failed to bind LiveExcel type \"%s\"", type.getDeclaredName()), null, e,
                        bindingContext);
            }
        }
    }

    public void bind(XlsModuleOpenClass module, RulesModuleBindingContext bindingContext) {
        for (Type type : types) {
            IOpenClass dataType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, type.getDeclaredName());
            try {
                if (dataType != null) {
                    addAllFileds((DatatypeOpenClass) dataType, type, bindingContext);
                    module.addType(ISyntaxConstants.THIS_NAMESPACE, dataType);
                }
            } catch (Exception e) {
                BindHelper.processError(String.format("Failed to bind LiveExcel type \"%s\"", type.getDeclaredName()), null, e,
                        bindingContext);
            }
        }
    }

    private void addAllFileds(DatatypeOpenClass openlType, Type leType, RulesModuleBindingContext bindingContext) {
        Map<String, FieldDescription> fieldMap = new HashMap<String, FieldDescription>();
        for (MappedProperty property : leType.getChilds()) {
            DatatypeOpenField field = new DatatypeOpenField(openlType, property.getDeclaredName(), TypeUtils.getOpenClass(
                    property.getTypeName(), bindingContext));
            fieldMap.put(property.getDeclaredName(), new FieldDescription(field));
            openlType.addField(field);
        }
        openlType.addMethod(new OpenFieldsConstructor(openlType));
        SimpleBeanByteCodeGenerator byteCodeGenerator = new SimpleBeanByteCodeGenerator(openlType.getPackageName()
                + "." + openlType.getName(), fieldMap);
        openlType.setInstanceClass(byteCodeGenerator.generateAndLoadBeanClass());
    }
}
