package org.openl.rules.table.xls.builder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMember;
import org.openl.types.impl.ExecutableMethod;
import org.openl.util.StringUtils;

/**
 * The class is responsible for creating test method tables in excel sheets. Given all necessary data (parameter names
 * and titles, result column description, table being tested and testmethod name) it just creates a new table in the
 * given sheet.
 *
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
public class TestTableBuilder extends TableBuilder {

    /** Default result parameter title. */
    private static final String RESULT_PARAM_TITLE = "Result";

    /** Test method name postfix */
    private static final String TESTMETHOD_NAME_POSTFIX = "Test";

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public TestTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    /**
     * Returns executable method from node.
     *
     * @param executableTsn Executable node
     * @return executable method from node
     */
    private static ExecutableMethod getExecutableMethod(TableSyntaxNode executableTsn) {
        Objects.requireNonNull(executableTsn, "executableTsn can't be null.");

        if (!executableTsn.isExecutableNode()) {
            throw new IllegalArgumentException("Syntax node is not executable node.");
        }
        IOpenMember member = executableTsn.getMember();
        if (member != null) {
            // as node is executable it will be instance of ExecutableRulesMethod
            return (ExecutableMethod) member;
        }
        return null;
    }

    /**
     * Returns table header.
     *
     * @param executableNode Executable node
     * @param technicalName Technical name of the table. If <code>null</code> or empty we get default technical name. It
     *            is building with table name and postfix 'Test'.
     * @return table header
     */
    public static String getHeader(TableSyntaxNode executableNode, String technicalName) {
        String result = null;
        ExecutableMethod executableMethod = getExecutableMethod(executableNode);
        if (executableMethod != null) {
            String tableName = executableMethod.getName();
            if (StringUtils.isNotEmpty(technicalName)) {
                result = IXlsTableNames.TEST_TABLE + " " + tableName + " " + technicalName;
            } else {
                result = IXlsTableNames.TEST_TABLE + " " + tableName + " " + getDefaultTechnicalName(executableMethod);
            }

        }
        return result;
    }

    /**
     *
     * @param executableMethod
     * @return Default technical name for new test table. It is build from <code>ExecutableRulesMethod</code> name and
     *         postfix 'Test'.
     */
    private static String getDefaultTechnicalName(ExecutableMethod executableMethod) {
        String tableName = executableMethod.getName();
        return tableName + TESTMETHOD_NAME_POSTFIX;
    }

    /**
     * Gets the default technical name for new test table. At first we get the executable method from
     * <code>TableSyntaxNode</code> and if it is not <code>null</code> calls
     * {@link #getDefaultTechnicalName(ExecutableMethod)}.
     *
     * @param executableNode <code>TableSyntaxNode</code> that is executable (see {@link Executable})from which we tries
     *            to get the <code>ExecutableRulesMethod</code>.
     * @return Default technical name for new test table. It is build from table name and postfix 'Test'.
     */
    public static String getDefaultTechnicalName(TableSyntaxNode executableNode) {
        String result = null;
        ExecutableMethod executableMethod = getExecutableMethod(executableNode);
        if (executableMethod != null) {
            result = getDefaultTechnicalName(executableMethod);
        }
        return result;
    }

    /**
     * Returns table parameters.
     *
     * @param executableNode Executable node
     * @return table parameters
     */
    public static Map<String, String> getParams(TableSyntaxNode executableNode) {
        ExecutableMethod executableMethod = getExecutableMethod(executableNode);
        if (executableMethod != null) {
            Map<String, String> params = new LinkedHashMap<>();
            IMethodSignature tableHeaderSignature = executableMethod.getHeader().getSignature();
            for (int i = 0; i < tableHeaderSignature.getNumberOfParameters(); i++) {
                String paramName = tableHeaderSignature.getParameterName(i);
                params.put(paramName, id2title(paramName));
            }
            return params;
        }
        return null;
    }

    private static String id2title(String id) {
        StringBuilder sb = new StringBuilder();
        boolean space = true;
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (Character.isWhitespace(c) || c == '_') {
                if (!space) {
                    space = true;
                    sb.append(" ");
                }
            } else if (space || Character.isUpperCase(c)) {
                space = false;
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        } else {
            return id;
        }
        return sb.toString().trim();
    }

    /**
     * Writes test table parameters.
     *
     * @param params test table parameters
     * @param resultTitle result parameter title
     *
     * @throws IllegalArgumentException if params is null
     * @throws IllegalStateException if method is called without prior <code>beginTable()</code> call
     */
    public void writeParams(Map<String, String> params, String resultTitle) {
        Objects.requireNonNull(params, "params can't be null.");
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        if (!params.containsKey(TestMethodHelper.EXPECTED_RESULT_NAME)) {
            params.put(TestMethodHelper.EXPECTED_RESULT_NAME,
                StringUtils.isBlank(resultTitle) ? RESULT_PARAM_TITLE : resultTitle);
        }
        int column = 0;
        Set<String> names = params.keySet();
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
            String name = iterator.next();
            writeCell(column, getCurrentRow(), 1, 1, name);
            String title = params.get(name);
            writeCell(column, getCurrentRow() + 1, 1, 1, title);
            column++;
        }
        if (!params.isEmpty()) {
            incCurrentRow(2);
        }
    }
}
