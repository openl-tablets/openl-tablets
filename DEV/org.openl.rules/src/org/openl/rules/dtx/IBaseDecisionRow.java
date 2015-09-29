/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dtx;

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
    IDecisionTableParameterInfo getParameterInfo(int i);

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
