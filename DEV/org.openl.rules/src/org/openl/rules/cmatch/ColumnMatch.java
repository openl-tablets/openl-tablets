package org.openl.rules.cmatch;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

@Executable
public class ColumnMatch extends ExecutableRulesMethod {
    @Getter
    @Setter
    private List<TableColumn> columns;
    @Getter
    @Setter
    private List<TableRow> rows;

    @Getter
    @Setter
    private Object[] returnValues;
    @Getter
    @Setter
    private MatchNode checkTree;

    @Getter
    @Setter
    private IMatchAlgorithmExecutor algorithmExecutor;

    // WEIGHT algorithm
    @Getter
    @Setter
    private MatchNode totalScore;
    @Getter
    @Setter
    private int[] columnScores;

    public ColumnMatch() {
        super(null, null);
    }

    public ColumnMatch(IOpenMethodHeader header, ColumnMatchBoundNode node) {
        super(header, node);
        initProperties(getSyntaxNode().getTableProperties());
    }

    public IOpenSourceCodeModule getAlgorithm() {
        return ((ColumnMatchBoundNode) getBoundNode()).getAlgorithm();
    }

    @Override
    public BindingDependencies getDependencies() {
        var dependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(dependencies);
        return dependencies;
    }

    @Override
    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        var result = algorithmExecutor.invoke(this, params, env);
        if (result == null) {
            Class<?> type = getHeader().getType().getInstanceClass();
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type " + type.getName());
            }
        }
        return result;
    }

}
