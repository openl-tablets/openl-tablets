package org.openl.rules.calc.result.gen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.datatype.gen.BeanByteCodeGenerator;
import org.openl.rules.datatype.gen.DefaultFieldDescription;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.bean.writers.ClassDescriptionWriter;
import org.openl.rules.datatype.gen.bean.writers.ConstructorWithParametersWriter;
import org.openl.rules.datatype.gen.bean.writers.CustomSpreadsheetResultAnnotationWriter;
import org.openl.rules.table.Point;
import org.openl.util.generation.CustomSpreadsheetResultJavaGenerator;

/**
 * Byte code generator for custom spreadsheet results.<br>
 * 
 * Generates class that is child of {@link SpreadsheetResult} with decorator typed getter methods <br>
 * for each spreadsheet cell.
 * 
 * @author DLiauchuk
 */
public class CustomSpreadsheetResultByteCodeGenerator extends BeanByteCodeGenerator {
    
    private final Class<?> superClass = SpreadsheetResult.class;
    
    /** fields of the {@link SpreadsheetResult} that will be used in constructor of generating class */
    private static final Map<String, FieldDescription> spreadsheetResultFields;
    
    static {
        /** populate with fields descriptions*/
        spreadsheetResultFields = new LinkedHashMap<String, FieldDescription>();
        spreadsheetResultFields.put("results", new DefaultFieldDescription(Object[][].class));
        spreadsheetResultFields.put("rowNames", new DefaultFieldDescription(String[].class));
        spreadsheetResultFields.put("columnNames", new DefaultFieldDescription(String[].class));
        spreadsheetResultFields.put("fieldsCoordinates", new DefaultFieldDescription(Map.class));
    }

    private Map<String, FieldDescription> cellFieldsDescription;
    
    private Map<String, Point> fieldCoordinates;
    
    public CustomSpreadsheetResultByteCodeGenerator(String canonicalBeanName, Map<String, FieldDescription> cellFieldsDescription) {
        this(canonicalBeanName, cellFieldsDescription, null);
    }
     
    public CustomSpreadsheetResultByteCodeGenerator(String canonicalBeanName, Map<String, FieldDescription> cellFieldsDescription, 
            Map<String, Point> fieldCoordinates) {
        super(canonicalBeanName);
        
        this.cellFieldsDescription = new HashMap<String, FieldDescription>(cellFieldsDescription);
        
        boolean generateSetters = false;
        if (fieldCoordinates != null && !fieldCoordinates.isEmpty()) {
            this.fieldCoordinates = new HashMap<String, Point>(fieldCoordinates);
            generateSetters = true;
        }
        
        initWriters(generateSetters);
    }

    private void initWriters(boolean generateSetters) {  
        /** writer for the class description*/
        addWriter(new ClassDescriptionWriter(getBeanNameWithPackage(), superClass));
        
        /** writer for customspreadsheet result open class annotation **/
        addWriter(new CustomSpreadsheetResultAnnotationWriter());
        
        /** writer for the constructor with parent parameters*/
        addWriter(new ConstructorWithParametersWriter(getBeanNameWithPackage(), superClass, 
            new HashMap<String, FieldDescription>(), spreadsheetResultFields, spreadsheetResultFields));
        
        /** writer for generating decorator getter methods for spreadsheet cells*/
        addWriter(new DecoratorMethodWriter(getBeanNameWithPackage(), cellFieldsDescription, 
            CustomSpreadsheetResultJavaGenerator.SPREADSHEET_METHOD, "get"));
        
        if (generateSetters) {
            /** default constructor writer*/
            addWriter(new DefaultConstructorWriter(getBeanNameWithPackage(), superClass, getMaxPoint()));
            
            addWriter(new SettersWriter(getBeanNameWithPackage(), cellFieldsDescription, fieldCoordinates));
        }
    }
    
    private Point getMaxPoint() {
        int[] colMas = getColumns(); 
        Arrays.sort(colMas);
        int maxColumn = colMas[colMas.length - 1];
        
        int[] rowMas = getRows(); 
        Arrays.sort(rowMas);
        int maxRow = rowMas[rowMas.length - 1];
        
        return new Point(maxColumn, maxRow);
    }

    private int[] getRows() {
        int[] mas = new int[fieldCoordinates.size()];
        int i = 0;
        for (Point point : fieldCoordinates.values()) {
            mas[i] = point.getRow();
            i++;
        }
        return mas;
    }

    private int[] getColumns() {
        int[] mas = new int[fieldCoordinates.size()];
        int i = 0;
        for (Point point : fieldCoordinates.values()) {
            mas[i] = point.getColumn();
            i++;
        }
        return mas;
    }
}
