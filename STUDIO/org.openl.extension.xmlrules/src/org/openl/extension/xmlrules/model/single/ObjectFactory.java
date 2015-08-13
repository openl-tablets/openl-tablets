package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    public ExtensionModuleInfo createModule() {
        return new ExtensionModuleInfo();
    }

    public SheetInfo createSheet() {
        return new SheetInfo();
    }

    public TypeImpl createType() {
        return new TypeImpl();
    }

    public DataInstanceImpl createDataInstance() {
        return new DataInstanceImpl();
    }

    public TableImpl createTable() {
        return new TableImpl();
    }

    public FunctionImpl createFunction() {
        return new FunctionImpl();
    }

    public Cells createCells() {
        return new Cells();
    }

//    public SingleValue createSingleValue() {
//        return new SingleValue();
//    }
//
//    public ArrayValue createArrayValue() {
//        return new ArrayValue();
//    }

//    public ExpressionImpl createExpression() {
//        return new ExpressionImpl();
//    }
}
