package org.openl.rules.method;

import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.enumeration.RecalculateEnum;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.IUriMember;
import org.openl.rules.vm.CacheMode;
import org.openl.rules.vm.ResultNotFoundException;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public abstract class ExecutableRulesMethod extends ExecutableMethod implements ITablePropertiesMethod, IUriMember {

    private ITableProperties properties;
    // FIXME: it should be AMethodBasedNode but currently it will be
    // ATableBoundNode due to TestSuiteMethod instance of
    // ExecutableRulesMethod(but test table is firstly data table)
    private ATableBoundNode boundNode;

    private String uri;

    private boolean hasAliasTypeParams;
    private IOpenCast[] aliasDatatypesCasts;

    @Override
    public String getUri() {
        if (this.uri == null) {
            throw new IllegalStateException("Table uri is not defined in the method.");
        }
        return uri;
    }

    /**
     * Must be invoked from inherited class constructor only
     */
    protected final void setUri(String uri) {
        this.uri = uri;
    }

    public ExecutableRulesMethod(IOpenMethodHeader header, ATableBoundNode boundNode) {
        super(header);
        this.boundNode = boundNode;
        if (this.boundNode != null) {
            uri = boundNode.getTableSyntaxNode().getTable().getSource().getUri();
        }
        hasAliasTypeParams = false;
        if (header != null) {
            int i = 0;
            ICastFactory castFactory = new CastFactory();
            for (IOpenClass param : header.getSignature().getParameterTypes()) {
                if (param instanceof DomainOpenClass) {
                    hasAliasTypeParams = true;
                    if (aliasDatatypesCasts == null) {
                        aliasDatatypesCasts = new IOpenCast[header.getSignature().getNumberOfParameters()];
                    }
                    IOpenClass methodParam = header.getSignature().getParameterTypes()[i];
                    aliasDatatypesCasts[i++] = castFactory
                        .getCast(JavaOpenClass.getOpenClass(methodParam.getInstanceClass()), methodParam);
                }
            }
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
        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return invoke2(target, params, env);
        }
    };

    private Object invoke2(Object target, Object[] params, IRuntimeEnv env) {
        if (hasAliasTypeParams) {
            for (int i = 0; i < getSignature().getNumberOfParameters(); i++) {
                if (aliasDatatypesCasts[i] != null) {
                    aliasDatatypesCasts[i].convert(params[i]); // Validate alias
                    // datatypes
                }
            }
        }
        if (env instanceof SimpleRulesRuntimeEnv) {
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
            Object result;
            boolean isSimilarStep = false;
            boolean oldIsIgnoreRecalculate = simpleRulesRuntimeEnv.isIgnoreRecalculation();
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!RecalculateEnum.ALWAYS.equals(getRecalculateType())) {
                    if (simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        simpleRulesRuntimeEnv.getArgumentCachingStorage().makeForwardStepForOriginalCalculation(this);
                    }
                    if (!simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        isSimilarStep = simpleRulesRuntimeEnv.getArgumentCachingStorage().makeForwardStep(this);
                        if (isSimilarStep && RecalculateEnum.NEVER.equals(getRecalculateType())) {
                            return simpleRulesRuntimeEnv.getArgumentCachingStorage()
                                .getValueFromOriginalCalculation(this);
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
                    result = simpleRulesRuntimeEnv.getArgumentCachingStorage().findInCache(this, params);
                } catch (ResultNotFoundException e) {
                    result = innerInvoke(target, params, env);
                    if (CacheMode.READ_WRITE.equals(simpleRulesRuntimeEnv.getCacheMode())) {
                        simpleRulesRuntimeEnv.getArgumentCachingStorage().putToCache(this, params, result);
                    }
                }
            } else {
                result = innerInvoke(target, params, env);
            }
            simpleRulesRuntimeEnv.setIgnoreRecalculate(oldIsIgnoreRecalculate);
            if (!simpleRulesRuntimeEnv.isIgnoreRecalculation()) {
                if (!RecalculateEnum.ALWAYS.equals(getRecalculateType())) {
                    if (simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        simpleRulesRuntimeEnv.getArgumentCachingStorage()
                            .makeBackwardStepForOriginalCalculation(this, result);
                    }
                    if (isSimilarStep && !simpleRulesRuntimeEnv.isOriginalCalculation()) {
                        simpleRulesRuntimeEnv.getArgumentCachingStorage().makeBackwardStep(this);
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
            if (recalculateType == null) {
                recalculateType = RecalculateEnum.ALWAYS;
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

    @Override
    public Map<String, Object> getProperties() {
        if (getMethodProperties() != null) {
            return getMethodProperties().getAllProperties();
        }
        return null;

    }

    @Override
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
     * Overridden to get access to {@link TableSyntaxNode} from current implementation.
     */
    @Override
    public TableSyntaxNode getSyntaxNode() {
        if (boundNode != null) {
            return boundNode.getTableSyntaxNode();
        }

        return null;
    }
}
