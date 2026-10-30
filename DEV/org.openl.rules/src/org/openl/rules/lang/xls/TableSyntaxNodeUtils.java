package org.openl.rules.lang.xls;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TableSyntaxNodeUtils {

    /**
     * The name a reader knows a table by: the value of its {@code name} property, or else the name its header gives.
     */
    public static String getTableName(TableSyntaxNode tableSyntaxNode) {
        var tableProperties = tableSyntaxNode.getTableProperties();
        var name = tableProperties == null ? null : tableProperties.getName();
        return name != null
                ? name
                : str2name(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(), tableSyntaxNode.getNodeType());
    }

    // Kept as an open refactoring task: it needs a design change beyond this cleanup.
    @SuppressWarnings("java:S1135")
    // TODO: refactor
    // Pass the HeaderSyntaxNode of the tsn and gets it`s name
    // Update header parsing in all components on Binding phase
    // @author DLiauchuk
    public static String str2name(String methodHeader, XlsNodeTypes tableType) {
        var resultName = methodHeader;

        if (StringUtils.isBlank(resultName)) {
            resultName = "NO NAME";
        } else if (tableType == XlsNodeTypes.XLS_OTHER) {
            if (resultName.length() > 57) {
                resultName = resultName.substring(0, 57) + "...";
            }
        } else if (tableType == XlsNodeTypes.XLS_DATATYPE) {
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
        var mi = testMethod.getInfo();
        var tnode = (TableSyntaxNode) mi.getSyntaxNode();
        return getTableName(tnode);
    }
}
