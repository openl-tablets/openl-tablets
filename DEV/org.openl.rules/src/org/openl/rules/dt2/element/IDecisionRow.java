/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt2.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public interface IDecisionRow {

    String getName();

    IOpenMethod getMethod();
    
    IOpenSourceCodeModule getSourceCodeModule();

//    DecisionTableParameterInfo findParameterInfo(String name);

    DecisionTableParameterInfo getParameterInfo(int i);

    String[] getParamPresentation();

    IParameterDeclaration[] getParams();

    ILogicalTable getValueCell(int column);
    
    Object getParamValue(int paramIndex, int ruleN);
	boolean isEmpty(int ruleN);
	boolean hasFormula(int ruleN);
	int getNumberOfRules();
	int getNumberOfParams();
	void loadValues(Object[] dest, int offset, int ruleN, Object target,
			Object[] tableParams, IRuntimeEnv env);

	
	boolean hasFormulasInStorage();


    
    

    void clearParamValues();

    int numberOfParams();

    boolean isAction();

    boolean isCondition();

    void prepare(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentModule,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception;

    /**
     * @return Parsed table that contains this decision row.
     */
    ILogicalTable getDecisionTable();


}
