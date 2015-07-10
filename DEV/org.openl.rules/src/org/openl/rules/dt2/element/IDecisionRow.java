/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt2.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dtx.IBaseDecisionRow;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public interface IDecisionRow  extends IBaseDecisionRow{


    
    IOpenSourceCodeModule getSourceCodeModule();

//    DecisionTableParameterInfo findParameterInfo(String name);


    String[] getParamPresentation();


    ILogicalTable getValueCell(int column);
    
    Object getParamValue(int paramIndex, int ruleN);
	boolean hasFormula(int ruleN);

	
	boolean hasFormulasInStorage();


    
    

    void clearParamValues();


    boolean isAction();

    boolean isCondition();

    void prepare(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ComponentOpenClass componentModule,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception;

	void loadValues(Object[] dest, int offset, int ruleN, Object target,
			Object[] tableParams, IRuntimeEnv env);

    
	boolean isEqual(int rule1, int rule2);
	
}
