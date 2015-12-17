package org.openl.rules.datatype.gen.types.writers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.datatype.gen.FieldDescription;

public class DateTypeWriter extends ObjectTypeWriter {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a"); 
    
    @Override
    protected String updateValue(FieldDescription fieldType) {
        Date date = (Date) fieldType.getDefaultValue();
        return simpleDateFormat.format(date);
    }

}
