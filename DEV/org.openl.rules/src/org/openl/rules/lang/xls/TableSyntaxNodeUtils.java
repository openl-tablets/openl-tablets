package org.openl.rules.lang.xls;

import java.util.Date;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.formatters.Formats;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

public final class TableSyntaxNodeUtils {

    private TableSyntaxNodeUtils() {
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode, int i, Formats formats) {
        return getTableDisplayValue(tableSyntaxNode, i, null, formats);
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode,
                                                int i,
                                                OverloadedMethodsDictionary dictionary,
                                                Formats formats) {

        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();

        String display = null;
        String name = null;

        if (tableProperties != null) {
            name = tableProperties.getName();
            display = name;
        }

        if (name == null) {
            name = str2name(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(),
                    tableSyntaxNode.getNodeType());
        }

        if (display == null) {
            display = tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue();
        }

        String sfx = i < 2 ? "" : " (" + i + ")";
        String dimensionInfo = StringUtils.EMPTY;

        if (dictionary != null && tableProperties != null && tableSyntaxNode
                .getMember() instanceof IOpenMethod && dictionary.contains((IOpenMethod) tableSyntaxNode.getMember())) {

            if (dictionary.getAllMethodOverloads((IOpenMethod) tableSyntaxNode.getMember()).size() > 1) {
                // Add dimension properties info only if there are more than one table in dictionary.
                // For single table don`t add this info.
                //
                String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();

                for (String dimensionalPropertyName : dimensionalPropertyNames) {
                    String value;

                    Object propertyValue = tableProperties.getPropertyValue(dimensionalPropertyName);
                    if (formats != null && propertyValue instanceof Date) {
                        value = formats.formatDateOrDateTime((Date) propertyValue);
                    } else {
                        value = tableProperties.getPropertyValueAsString(dimensionalPropertyName);
                    }

                    if (StringUtils.isNotEmpty(value)) {
                        String propertyInfo = dimensionalPropertyName + "=" + value;
                        dimensionInfo = dimensionInfo + (StringUtils.isEmpty(dimensionInfo) ? StringUtils.EMPTY
                                : ", ") + propertyInfo;
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(dimensionInfo)) {
            sfx = sfx + " [" + dimensionInfo + "]";
        }

        return new String[]{name + sfx, display + sfx, display + sfx};
    }

    // TODO: refactor
    // Pass the HeaderSyntaxNode of the tsn and gets it`s name
    // Update header parsing in all components on Binding phase
    // @author DLiauchuk
    public static String str2name(String methodHeader, XlsNodeTypes tableType) {
        String resultName = methodHeader;

        if (StringUtils.isBlank(resultName)) {
            resultName = "NO NAME";
        } else if (tableType.equals(XlsNodeTypes.XLS_OTHER)) {
            if (resultName.length() > 57) {
                resultName = resultName.substring(0, 57) + "...";
            }
        } else if (tableType.equals(XlsNodeTypes.XLS_DATATYPE)) {
            // Get the second word
            var first = StringUtils.firstNonSpace(resultName, 0, resultName.length()); // the first token
            first = StringUtils.first(resultName, first, resultName.length(), StringUtils::isSpaceOrControl); // space
            first = StringUtils.firstNonSpace(resultName, first, resultName.length()); // the second token
            var last = StringUtils.first(resultName, first, resultName.length(), StringUtils::isSpaceOrControl); // space
            if (last < 0) {
                last = resultName.length();
            }
            if (first > 0) {
                resultName = resultName.substring(first, last);
            }
        } else {
            // Get the last word except method arguments
            var last = StringUtils.first(resultName, 0, resultName.length(), x -> x == '('); // arguments
            if (last < 0) {
                last = resultName.length();
            }
            last = StringUtils.lastNonSpace(resultName, 0, last) + 1;
            var first = StringUtils.last(resultName, 0, last, StringUtils::isSpaceOrControl);
            resultName = resultName.substring(first + 1, last);
        }

        return resultName.trim();
    }

    public static String getTestName(IOpenMethod testMethod) {
        IMemberMetaInfo mi = testMethod.getInfo();
        TableSyntaxNode tnode = (TableSyntaxNode) mi.getSyntaxNode();
        return getTableDisplayValue(tnode, 0, null)[INamedThing.SHORT];
    }
}
