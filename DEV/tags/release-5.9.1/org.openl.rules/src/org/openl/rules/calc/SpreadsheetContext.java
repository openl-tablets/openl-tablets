package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.calc.element.SpreadsheetRangeField;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.types.IOpenField;

public class SpreadsheetContext extends ComponentBindingContext {

    public SpreadsheetContext(IBindingContext delegate, ComponentOpenClass type) {
        super(delegate, type);
    }

    @Override
    public IOpenField findRange(String namespace, String rangeStartName, String rangeEndName) throws AmbiguousVarException,
                                                                                             FieldNotFoundException {
        
        String key = namespace + ":" + rangeStartName + ":" + rangeEndName;
        IOpenField res = ranges.get(key); 
        if (res != null)
            return res;
        
        IOpenField fstart = findVar(namespace, rangeStartName, true);
        
        if (fstart == null)
            throw new FieldNotFoundException("Can not find range start: ", rangeStartName, null);
        
        IOpenField fend = findVar(namespace, rangeEndName, true);
        
        if (fend == null)
            throw new FieldNotFoundException("Can not find range end: ", rangeEndName, null);

        if (!(fstart instanceof SpreadsheetCellField))
            throw new FieldNotFoundException("Range start must point to the cell: ", rangeStartName, null);
        
        if (!(fend instanceof SpreadsheetCellField))
            throw new FieldNotFoundException("Range end must point to the cell: ", rangeEndName, null);
        
        
        res = new SpreadsheetRangeField(key, (SpreadsheetCellField)fstart, (SpreadsheetCellField)fend);
        
        ranges.put(key, res);
        
        return res;
    }
    
    
    
    
    Map<String, IOpenField> ranges = new HashMap<String, IOpenField>();
    
    

}
