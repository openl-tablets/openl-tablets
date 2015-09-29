/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dtx.IBaseDecisionRow;
import org.openl.rules.dtx.IDecisionTableParameterInfo;
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
public interface IDecisionRow extends IBaseDecisionRow{

    String getName();

    IOpenMethod getMethod();
    
    IOpenSourceCodeModule getSourceCodeModule();

//    DecisionTableParameterInfo findParameterInfo(String name);

    IDecisionTableParameterInfo getParameterInfo(int i);

    String[] getParamPresentation();

    IParameterDeclaration[] getParams();

    Object[][] getParamValues();
    
    

    void clearParamValues();


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
