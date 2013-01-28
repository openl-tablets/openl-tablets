package org.openl.rules.method;

import java.util.Map;

import org.openl.rules.enumeration.Recalculation;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.vm.ResultNotFoundException;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.ExecutableMethod;
import org.openl.vm.IRuntimeEnv;

public abstract class ExecutableRulesMethod extends ExecutableMethod {

    private ITableProperties properties;
    // FIXME: it should be AMethodBasedNode but currently it will be
    // ATableBoundNode due to TestSuiteMethod instance of
    // ExecutableRulesMethod(but test table is firstly data table)
    private ATableBoundNode boundNode;

    public ExecutableRulesMethod(IOpenMethodHeader header, ATableBoundNode boundNode) {
        super(header);
        this.boundNode = boundNode;
    }

    private Boolean cacheble = null;

    protected boolean isMethodCacheable() {
        if (cacheble == null) {
            if (getMethodProperties() == null) {
                cacheble = Boolean.FALSE;
            } else {
                Boolean cacheable = (Boolean) getMethodProperties().getCacheable();
                cacheble = Boolean.TRUE.equals(cacheable);
            }
        }
        return cacheble.booleanValue();
    }

    @Override
    public final Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (env instanceof SimpleRulesRuntimeEnv) {
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
            Object result = null;
            boolean isSimilarStep = false;
            boolean oldIsIgnoreRecalculation = simpleRulesRuntimeEnv.isIgnoreRecalculation();
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!Recalculation.ALWAYS.equals(getRecalculationType())) {
                    simpleRulesRuntimeEnv.registerForwardOriginalCalculationStep(this);
                    if (!simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        isSimilarStep = simpleRulesRuntimeEnv.registerForwardStep(this);
                        if (isSimilarStep && Recalculation.NEVER.equals(getRecalculationType())) {
                            return simpleRulesRuntimeEnv.getResultFromOriginalCalculation(this);
                        }
                    }
                } else {
                    if (Recalculation.ALWAYS.equals(getRecalculationType())){
                        simpleRulesRuntimeEnv.setIgnoreRecalculation(true);
                    }
                }
            }
            if (simpleRulesRuntimeEnv.isMethodArgumentsCacheEnable() && isMethodCacheable()) {
                try {
                    result = simpleRulesRuntimeEnv.findInCache(this, params);
                } catch (ResultNotFoundException e) {
                    result = innerInvoke(target, params, env);
                    simpleRulesRuntimeEnv.putToCache(this, params, result);
                }
            } else {
                result = innerInvoke(target, params, env);
            }
            simpleRulesRuntimeEnv.setIgnoreRecalculation(oldIsIgnoreRecalculation);
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!Recalculation.ALWAYS.equals(getRecalculationType())) {
                    simpleRulesRuntimeEnv.registerBackwardOriginalCalculationStep(this, result);
                    if (isSimilarStep && !simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        simpleRulesRuntimeEnv.registerBackwardStep(this);
                    }
                }
            }
            return result;
        } else {
            return innerInvoke(target, params, env);
        }
    }

    Recalculation recalculationType = null;

    private Recalculation getRecalculationType() {
        if (recalculationType == null) {
            if (getMethodProperties() == null) {
                recalculationType = Recalculation.ALWAYS;
            } else {
                recalculationType = (Recalculation) getMethodProperties().getRecalculation();
            }
        }
        return recalculationType;
    }

    protected abstract Object innerInvoke(Object target, Object[] params, IRuntimeEnv env);

    public void setBoundNode(ATableBoundNode node) {
        this.boundNode = node;
    }

    public ATableBoundNode getBoundNode() {
        return boundNode;
    }

    public Map<String, Object> getProperties() {
        if (getMethodProperties() != null) {
            return getMethodProperties().getAllProperties();
        }
        return null;

    }

    public ITableProperties getMethodProperties() {
        return properties;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    protected void initProperties(ITableProperties tableProperties) {
        this.properties = tableProperties;
    }

    /**
     * Overridden to get access to {@link TableSyntaxNode} from current
     * implenmentation.
     */
    public TableSyntaxNode getSyntaxNode() {
        if (boundNode != null) {
            return boundNode.getTableSyntaxNode();
        }

        return null;
    }
}
