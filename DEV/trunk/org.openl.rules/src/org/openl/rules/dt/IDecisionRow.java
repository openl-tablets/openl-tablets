/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
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
    DTParameterInfo findParameterInfo(String name);

    IOpenMethod getMethod();

    String getName();

    DTParameterInfo getParameterInfo(int i);

    String[] getParamPresentation();

    IParameterDeclaration[] getParams();

    // public IOpenMethod getCode();

    Object[][] getParamValues();

    IOpenSourceCodeModule getSourceCodeModule();

    boolean isAction();

    boolean isCondition();

    int numberOfParams();

    void prepare(IOpenClass methodType, IMethodSignature signature, OpenL openl, ModuleOpenClass dtModule,
            IBindingContextDelegator cxtd, RuleRow ruleRow) throws Exception;

    /**
     * @return Parsed table that contains this decision row.
     */
    ILogicalTable getDecisionTable();
}
