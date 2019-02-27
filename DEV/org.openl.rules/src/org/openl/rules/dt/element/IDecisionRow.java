package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public interface IDecisionRow extends IBaseDecisionRow {

    IOpenSourceCodeModule getSourceCodeModule();

    String[] getParamPresentation();

    Object getParamValue(int paramIndex, int ruleN);

    boolean hasFormula(int ruleN);

    boolean hasFormulas();

    void clearParamValues();

    boolean isAction();

    boolean isCondition();

    void prepare(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            IBindingContext bindingContext,
            RuleRow ruleRow,
            TableSyntaxNode tableSyntaxNode) throws Exception;

    void loadValues(Object[] dest, int offset, int ruleN, Object target, Object[] tableParams, IRuntimeEnv env);

    boolean isEqual(int rule1, int rule2);

}
