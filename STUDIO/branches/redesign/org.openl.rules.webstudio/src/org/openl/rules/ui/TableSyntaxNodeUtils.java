package org.openl.rules.ui;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.types.IOpenMethod;

public class TableSyntaxNodeUtils {

    private static final String ROUND_BRACKETS_WITH_ANY_TEXT = "\\(.*\\)";
    private static final String DISPLAY_TABLE_PROPERTY_NAME = "display";    

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode) {
        
        return getTableDisplayValue(tableSyntaxNode, 0);
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode, int i) {
        
        return getTableDisplayValue(tableSyntaxNode, i, null);
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode, int i, OverloadedMethodsDictionary dictionary) {
        
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        
        String display = null;
        String name = null;

        if (tableProperties != null) {

            name = tableProperties.getName();
            // FIXME: What a property name 'display'??? there is no such property.
            display = tableProperties.getPropertyValueAsString(DISPLAY_TABLE_PROPERTY_NAME);
        
            if (display == null) {
                display = name;
            }
        }

        if (name == null) {
            name = str2name(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(), tableSyntaxNode.getNodeType());
        }

        if (display == null) {
            display = str2display(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(), tableSyntaxNode.getType());
        }

        String sfx = (i < 2 ? "" : " (" + i + ")");
        String dimensionInfo = StringUtils.EMPTY;

        if (dictionary != null && tableProperties != null && tableSyntaxNode.getMember() instanceof IOpenMethod
                && dictionary.contains((IOpenMethod) tableSyntaxNode.getMember())) {
            
            if (dictionary.getAllMethodOverloads((IOpenMethod)tableSyntaxNode.getMember()).size() > 1) {
                // Add dimension properties info only if there are more than one table in dictionary.
                // For single table don`t add this info.
                //   
                String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();

                for (String dimensionalPropertyName : dimensionalPropertyNames) {
                    String value = tableProperties.getPropertyValueAsString(dimensionalPropertyName);

                    if (!StringUtils.isEmpty(value)) {
                        String propertyInfo = StringUtils.join(new Object[] { dimensionalPropertyName, "=", value });
                        dimensionInfo = StringUtils.join(new Object[] { dimensionInfo,
                                StringUtils.isEmpty(dimensionInfo) ? StringUtils.EMPTY : ", ", propertyInfo });
                    }
                }
            }            
        }

        if (!StringUtils.isEmpty(dimensionInfo)) {
            sfx = StringUtils.join(new Object[] { sfx, StringUtils.isEmpty(sfx) ? StringUtils.EMPTY : " ", "[",
                    dimensionInfo, "]" });
        }

        return new String[] { name + sfx, display + sfx, display + sfx };
    }

    private static String str2display(String src, String type) {

        return src;
    }
    
    // TODO: refactor
    // Pass the HeaderSyntaxNode of the tsn and gets it`s name
    // Update header parsing in all components on Binding phase
    // @author DLiauchuk
    public static String str2name(String methodHeader, XlsNodeTypes tableType) {
        String resultName = methodHeader;
        if (StringUtils.isBlank(resultName)) {
            resultName = "NO NAME";
        } else if (tableType.equals(XlsNodeTypes.XLS_DATATYPE)) {
            String[] tokens = StringUtils.split(resultName.replaceAll(ROUND_BRACKETS_WITH_ANY_TEXT, ""));
            // ensure that the appropriate index exists
            //
            if (tokens.length > DatatypeNodeBinder.TYPE_INDEX) {
                resultName = tokens[DatatypeNodeBinder.TYPE_INDEX].trim();
            }            
        } else if (tableType.equals(XlsNodeTypes.XLS_DT) || tableType.equals(XlsNodeTypes.XLS_SPREADSHEET)
                || tableType.equals(XlsNodeTypes.XLS_TBASIC) || tableType.equals(XlsNodeTypes.XLS_COLUMN_MATCH)
                || tableType.equals(XlsNodeTypes.XLS_DATA) || tableType.equals(XlsNodeTypes.XLS_DATATYPE)
                || tableType.equals(XlsNodeTypes.XLS_METHOD) || tableType.equals(XlsNodeTypes.XLS_TEST_METHOD)
                || tableType.equals(XlsNodeTypes.XLS_RUN_METHOD)) {

            String[] tokens = StringUtils.split(resultName.replaceAll(ROUND_BRACKETS_WITH_ANY_TEXT, ""));
            resultName = tokens[tokens.length - 1].trim();
        }
        return resultName;
    }

    /**
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if this table is internal service table, that was auto generated. 
     */
    public static boolean isAutoGeneratedTable(TableSyntaxNode tsn) {
        if (tsn.getMember() instanceof IOpenMethod) {
            if (((IOpenMethod) tsn.getMember()).getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME)) {
                return true;// This is dimension properties gap/overlap
                            // validation table
            }
        }
        return false;
    }
}
