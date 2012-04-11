/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;

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

    Object[][] getParamValues();

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
