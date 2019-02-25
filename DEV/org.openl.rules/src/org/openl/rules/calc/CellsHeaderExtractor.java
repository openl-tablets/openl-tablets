package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;

/**
 * Extractor for values that are represented as column and row names in spreadsheet.
 * 
 * @author DLiauchuk
 *
 */
public class CellsHeaderExtractor {
    private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
    private String[] rowNames;
    private String[] columnNames;
    private Set<String> dependentSpreadsheetTypes;
    
    /** table representing column section in the spreadsheet **/
    private final ILogicalTable columnNamesTable;

    /** table representing row section in the spreadsheet **/
    private final ILogicalTable rowNamesTable;

    /** Spreadsheet signature */
    private final String spreadsheetSignature;
    
    // regex that represents the next line:
    // [any_symbols] : SpreadsheetResult<custom_spreadsheet_result_name>
    //
    // package scope just for tests
    static final String DEPENDENT_CSR_REGEX = "^.*\\s*:\\s*SpreadsheetResult[^\\s\\[\\]].*";

    /**
     * Pattern that represents the next line:
     * Spreadsheet SpreadsheetResult<custom_spreadsheet_result_name> <name_and_params_declaration>
     */
    private static final Pattern CSR_IN_RETURN_PATTERN = Pattern.compile("\\s*Spreadsheet\\s*SpreadsheetResult([^\\s\\[\\]]+).+");

    /**
     * Pattern that represents parameters of the spreadsheet
     */
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("\\((.+)\\)");

    /**
     * Pattern that represents custom spreadsheet type parameter
     */
    private static final Pattern CSR_TYPE_PATTERN = Pattern.compile("\\s*SpreadsheetResult([^\\s\\[\\]]+).+");
    
    public CellsHeaderExtractor(String spreadsheetSignature, ILogicalTable columnNamesTable, ILogicalTable rowNamesTable) {
        this.spreadsheetSignature = spreadsheetSignature;
        this.columnNamesTable = columnNamesTable;
        this.rowNamesTable = rowNamesTable;
    }

    public ILogicalTable getColumnNamesTable() {
        return columnNamesTable;
    }

    public int getWidth() {
        return (columnNamesTable == null) ? 0 : columnNamesTable.getWidth();
    }


    public ILogicalTable getRowNamesTable() {
        return rowNamesTable;
    }

    public int getHeight() {
        return (rowNamesTable == null) ? 0 : rowNamesTable.getHeight();
    }

    public String[] getRowNames() {
        if (rowNames == null) {
            int height = getHeight();
            rowNames = new String[height];
            for (int row = 0; row < height; row++) {
                IGridTable nameCell = rowNamesTable.getRow(row).getColumn(0).getSource();
                rowNames[row] = nameCell.getCell(0, 0).getStringValue();
            }
        }
        return rowNames;
    }
    
    public String[] getColumnNames() {
        if (columnNames == null) {
            int width = getWidth();
            columnNames = new String[width];
            for (int col = 0; col < width; col++) {
                IGridTable nameCell = columnNamesTable.getColumn(col).getRow(0).getSource();
                columnNames[col] = nameCell.getCell(0, 0).getStringValue();
            }
        }
        return columnNames;
    }
    
    public Set<String> getDependentSignatureSpreadsheetTypes() {
        if (dependentSpreadsheetTypes == null) {
            dependentSpreadsheetTypes = new HashSet<String>();
            dependentSpreadsheetTypes.addAll(getSignatureDependencies(spreadsheetSignature));
        }
        return dependentSpreadsheetTypes;
    }

    // package scope just for tests
    static List<String> getSignatureDependencies(String signature) {
        List<String> dependentSpreadsheets = new ArrayList<String>();

        Matcher matcher = CSR_IN_RETURN_PATTERN.matcher(signature);
        if (matcher.matches()) {
            dependentSpreadsheets.add(matcher.group(1));
        }

        matcher = PARAMETERS_PATTERN.matcher(signature);
        if (matcher.find()) {
            String allParams = matcher.group(1);
            for (String param : COMMA.split(allParams)) {
                Matcher paramMatcher = CSR_TYPE_PATTERN.matcher(param);
                if (paramMatcher.matches()) {
                    dependentSpreadsheets.add(paramMatcher.group(1));
                }
            }
        }

        return dependentSpreadsheets;
    }

}
