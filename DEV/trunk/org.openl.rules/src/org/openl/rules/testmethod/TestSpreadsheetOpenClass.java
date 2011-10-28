package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetSourceExtractor;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Open class for test that is testing {@link Spreadsheet}.<br><br>
 * 
 * In case spreadsheet returns {@link SpreadsheetResult} in its signature, it is possible to access<br>
 * any cell of spreadsheet for testing. By convention see {@link SpreadsheetStructureBuilder}.<br><br>
 * 
 * In case spreadsheet returns any specific cell by {@link SpreadsheetSourceExtractor#RETURN_NAME},<br>
 * only value can be tested by using {@link TestMethodHelper#EXPECTED_RESULT_NAME} field.
 * 
 * @author DLiauchuk
 * 
 * TODO: add the possibility to check cells as well when there is RETURN in spreadsheet.
 * Refactor, extend {@link TestMethodOpenClass}
 */
public class TestSpreadsheetOpenClass extends ADynamicClass {
    
    /** identifiers for the spreadsheet cells that are used for testing in test table*/
    private List<IdentifierNode[]> spreadsheetCellsForTest;
    
    public TestSpreadsheetOpenClass(String tableName, Spreadsheet testedSpreadsheet, List<IdentifierNode[]> columnIdentifiers) {
        super(null, tableName + "SpreadsheetTestClass", DynamicObject.class);
        
        if (columnIdentifiers != null) {
            this.spreadsheetCellsForTest = new ArrayList<IdentifierNode[]>();
            
            /** from column identifiers in test table, take only those that follows after the parameters of the tested method*/
            for (int j = testedSpreadsheet.getSignature().getParameterTypes().length; j < columnIdentifiers.size(); j++) {
                spreadsheetCellsForTest.add(columnIdentifiers.get(j));
            }
        }
        init(testedSpreadsheet);
    }
    
    @Override
    public Object newInstance(IRuntimeEnv env) {        
        return new DynamicObject(this);
    }
    
    public List<IdentifierNode[]> getSpreadsheetCellsForTest() {
        return new ArrayList<IdentifierNode[]>(spreadsheetCellsForTest);
    }
    
    private void init(Spreadsheet testedSpreadsheet) {
        /** add the fields from the signature of the testing method*/
        addParameterFields(testedSpreadsheet);
        
        /** add the fields from tested spreadsheet cells to give access to the cells from test table*/
        for (IOpenField field : testedSpreadsheet.getType().getFields().values()) {
            if (field.getName().startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
                IOpenField resultField = new DynamicObjectField(this, field.getName(), field.getType());
                addField(resultField);
            }
        }
        
        /** add expected result field if the return type of the spreadsheet is not a SpreadsheetResult*/
        if (!ClassUtils.isAssignable(testedSpreadsheet.getHeader().getType().getInstanceClass(), SpreadsheetResult.class, true)) {
            addExpectedResult(testedSpreadsheet);
        }
        
        /** add description field*/
        addDescription();

        /** add context field*/
        addContext();
        
        /** add expected error field*/
        addExpectedError();
    }
    
    private void addParameterFields(IOpenMethod testedMethod) {
        IOpenClass[] parameterTypes = testedMethod.getSignature().getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            String name = testedMethod.getSignature().getParameterName(i);
            IOpenField parameterField = new DynamicObjectField(this, name, parameterTypes[i]);

            addField(parameterField);
        }
    }
    
    private void addExpectedError() {
        IOpenField errorField = new DynamicObjectField(this,
            TestMethodHelper.EXPECTED_ERROR,
            JavaOpenClass.STRING);
        addField(errorField);
    }

    private void addContext() {
        IOpenField contextField = new DynamicObjectField(this,
            TestMethodHelper.CONTEXT_NAME,
            JavaOpenClass.getOpenClass(DefaultRulesRuntimeContext.class));
        addField(contextField);
    }

    private void addDescription() {
        IOpenField descriptionField = new DynamicObjectField(this,
            TestMethodHelper.DESCRIPTION_NAME,
            JavaOpenClass.STRING);
        addField(descriptionField);
    }

    private void addExpectedResult(IOpenMethod testedMethod) {
        IOpenField resultField = new DynamicObjectField(this,
            TestMethodHelper.EXPECTED_RESULT_NAME,
            testedMethod.getType());
        addField(resultField);
    }    
}
