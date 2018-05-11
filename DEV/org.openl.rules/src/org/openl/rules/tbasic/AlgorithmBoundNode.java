package org.openl.rules.tbasic;

import java.util.List;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.AlgorithmMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.tbasic.runtime.operations.OpenLEvaluationOperation;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

public class AlgorithmBoundNode extends AMethodBasedNode implements IMemberBoundNode {

    public AlgorithmBoundNode(TableSyntaxNode tsn, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(tsn, openl, header, module);
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        return Algorithm.createAlgorithm(getHeader(), this);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        if (!cxt.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new AlgorithmMetaInfoReader(this));
        }

        super.finalizeBind(cxt);
        AlgorithmBuilder builder = new AlgorithmBuilder(cxt, getAlgorithm(), getTableSyntaxNode());

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();
        builder.build(tableBody);

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody.getRows(1));
    }

    public Algorithm getAlgorithm() {
        return (Algorithm) getMethod();
    }
    
    @Override
    public void updateDependency(BindingDependencies dependencies) {
        Algorithm algorithm = getAlgorithm();
        if (algorithm != null) {
            List<RuntimeOperation> operations = algorithm.getAlgorithmSteps();
            if (operations != null) {
                for (RuntimeOperation step : operations) {
                    if (step instanceof OpenLEvaluationOperation) {
                        IMethodCaller methodCaller = ((OpenLEvaluationOperation) step).getOpenLStatement();
                        if (methodCaller instanceof CompositeMethod) {
                            ((CompositeMethod) methodCaller).updateDependency(dependencies);
                        }
                    }
                }
            }
        }
    }
}
