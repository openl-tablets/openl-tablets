package org.openl.rules.calc.result.gen;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.datatype.gen.BeanByteCodeGenerator;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.bean.writers.ClassDescriptionWriter;
import org.openl.rules.datatype.gen.bean.writers.ConstructorWithParametersWriter;

/**
 * Byte code generator for custom spreadsheet results.<br>
 * 
 * Generates class that is child of {@link SpreadsheetResult} with decorator typed getter methods <br>
 * for each spreadsheet cell.
 * 
 * @author DLiauchuk
 */
public class CustomSpreadsheetResultByteCodeGenerator extends BeanByteCodeGenerator {
    
    /** fields of the {@link SpreadsheetResult} that will be used in constructor of generating class */
    private static final Map<String, FieldDescription> spreadsheetResultFields;
    
    static {
        /** populate with fields descriptions*/
        spreadsheetResultFields = new LinkedHashMap<String, FieldDescription>();
        spreadsheetResultFields.put("results", new FieldDescription(Object[][].class));
        spreadsheetResultFields.put("rowNames", new FieldDescription(String[].class));
        spreadsheetResultFields.put("columnNames", new FieldDescription(String[].class));
        spreadsheetResultFields.put("fieldsCoordinates", new FieldDescription(Map.class));
    }

    private Map<String, FieldDescription> cellFieldsDescription;
    
    public CustomSpreadsheetResultByteCodeGenerator(String canonicalBeanName, Map<String, FieldDescription> cellFieldsDescription) {
        super(canonicalBeanName);
        
        this.cellFieldsDescription = new HashMap<String, FieldDescription>(cellFieldsDescription);
        
        initWriters();
    }

    private void initWriters() {  
        /** writer for the class description*/
        addWriter(new ClassDescriptionWriter(getBeanNameWithPackage(), SpreadsheetResult.class));
        
        /** writer for the constructor with parent parameters*/
        addWriter(new ConstructorWithParametersWriter(getBeanNameWithPackage(), SpreadsheetResult.class, 
            new HashMap<String, FieldDescription>(), spreadsheetResultFields, spreadsheetResultFields));
        
        /** writer for generating decorator getter methods for spreadsheet cells*/
        addWriter(new DecoratorMethodWriter(getBeanNameWithPackage(), cellFieldsDescription, "getFieldValue", "get"));
    }
}
