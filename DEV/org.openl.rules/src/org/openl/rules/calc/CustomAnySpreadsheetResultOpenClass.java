package org.openl.rules.calc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.calc.SpreadsheetResultBeanByteCodeGenerator.FieldDescription;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

public class CustomAnySpreadsheetResultOpenClass extends CustomSpreadsheetResultOpenClass {

    private final Set<CustomSpreadsheetResultOpenClass> anyOpenClasses = new HashSet<>();

    public CustomAnySpreadsheetResultOpenClass(String name, XlsModuleOpenClass module, ILogicalTable logicalTable, boolean spreadsheet) {
        super(name, module, logicalTable, spreadsheet);
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (openClass instanceof CustomSpreadsheetResultOpenClass) {
            anyOpenClasses.add((CustomSpreadsheetResultOpenClass) openClass);
        }
        super.updateWithType(openClass);
    }

    @Override
    protected byte[] generateBytecode(String beanClassName, List<FieldDescription> beanFields) {
        var classBytes = super.generateBytecode(beanClassName, beanFields);
        if (!anyOpenClasses.isEmpty()) {
            classBytes = new SwaggerSchemaSprBeanByteCodeDecorator(false, anyOpenClasses).decorate(classBytes);
        }
        return classBytes;
    }

}
