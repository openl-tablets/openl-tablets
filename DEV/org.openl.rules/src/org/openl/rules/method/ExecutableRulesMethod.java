package org.openl.rules.method;

import java.util.Map;

import org.openl.rules.enumeration.RecalculateEnum;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.vm.ResultNotFoundException;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.ExecutableMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public abstract class ExecutableRulesMethod extends ExecutableMethod implements ITablePropertiesMethod, TableUriMethod {

    private ITableProperties properties;
    // FIXME: it should be AMethodBasedNode but currently it will be
    // ATableBoundNode due to TestSuiteMethod instance of
    // ExecutableRulesMethod(but test table is firstly data table)
    private ATableBoundNode boundNode;

    private String tableUri;
    
    public String getTableUri(){
        if (this.tableUri == null){
            throw new IllegalStateException("Table uri doesn't defined in method!");
        }
        return tableUri;
    }
    
    public ExecutableRulesMethod(IOpenMethodHeader header, ATableBoundNode boundNode) {
        super(header);
        this.boundNode = boundNode;
        if (this.boundNode != null){
            tableUri = boundNode.getTableSyntaxNode().getTable().getSource().getUri();
        }
    }

    private Boolean cacheble = null;

    protected boolean isMethodCacheable() {
        if (cacheble == null) {
            if (getMethodProperties() == null) {
                cacheble = Boolean.FALSE;
            } else {
                Boolean cacheable = getMethodProperties().getCacheable();
                cacheble = Boolean.TRUE.equals(cacheable);
            }
        }
        return cacheble;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return Tracer.invoke(invoke2, target, params, env, this);
    }

    private Invokable invoke2 = new Invokable() {
        @Override public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return invoke2(target, params, env);
        }
    };

    private Object invoke2(Object target, Object[] params, IRuntimeEnv env) {
        if (env instanceof SimpleRulesRuntimeEnv) {
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
            Object result;
            boolean isSimilarStep = false;
            boolean oldIsIgnoreRecalculate = simpleRulesRuntimeEnv.isIgnoreRecalculation();
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!RecalculateEnum.ALWAYS.equals(getRecalculateType())) {
                    simpleRulesRuntimeEnv.registerForwardOriginalCalculationStep(this);
                    if (!simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        isSimilarStep = simpleRulesRuntimeEnv.registerForwardStep(this);
                        if (isSimilarStep && RecalculateEnum.NEVER.equals(getRecalculateType())) {
                            return simpleRulesRuntimeEnv.getResultFromOriginalCalculation(this);
                        }
                    }
                } else {
                    if (RecalculateEnum.ALWAYS.equals(getRecalculateType())) {
                        simpleRulesRuntimeEnv.setIgnoreRecalculate(true);
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
            simpleRulesRuntimeEnv.setIgnoreRecalculate(oldIsIgnoreRecalculate);
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!RecalculateEnum.ALWAYS.equals(getRecalculateType())) {
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

    RecalculateEnum recalculateType = null;

    private RecalculateEnum getRecalculateType() {
        if (recalculateType == null) {
            if (getMethodProperties() == null) {
                recalculateType = RecalculateEnum.ALWAYS;
            } else {
                recalculateType = getMethodProperties().getRecalculate();
            }
        }
        return recalculateType;
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
