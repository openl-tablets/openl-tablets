package org.openl.rules.dt;

import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;

/**
 * @author snshor
 *
 */
public interface IBaseDecisionRow {

    String getName();

    int getNumberOfParams();

    IParameterDeclaration[] getParams();

    int getNumberOfRules();

    boolean isEmpty(int ruleN);

    boolean hasFormula(int ruleN);

    Object getParamValue(int paramIdx, int ruleN);

    ILogicalTable getValueCell(int column);

    IOpenMethod getMethod();

    /**
     * @return Parsed table that contains this decision row.
     */
    ILogicalTable getDecisionTable();

    IOpenSourceCodeModule getSourceCodeModule();

}
