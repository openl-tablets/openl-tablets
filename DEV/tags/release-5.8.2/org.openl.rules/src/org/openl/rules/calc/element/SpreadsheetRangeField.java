package org.openl.rules.calc.element;

import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetRangeField extends  ASpreadsheetField{

    private final SpreadsheetCellField fstart;
    private final SpreadsheetCellField fend;

    public SpreadsheetRangeField(String name, SpreadsheetCellField fstart, SpreadsheetCellField fend) {
        super(fstart.getDeclaringClass(), name, JavaOpenClass.getOpenClass(SpreadsheetRangeObject.class));
        this.fstart = fstart;
        this.fend = fend;
        
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return new SpreadsheetRangeObject((SpreadsheetResultCalculator)target, fstart, fend);
    } 
    
     
    

}
