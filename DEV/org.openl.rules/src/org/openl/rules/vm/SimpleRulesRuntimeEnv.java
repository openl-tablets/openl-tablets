package org.openl.rules.vm;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class SimpleRulesRuntimeEnv extends SimpleRuntimeEnv {

    public SimpleRulesRuntimeEnv() {
        super();
    }

    public SimpleRulesRuntimeEnv(SimpleRuntimeEnv env) {
        super(env);
    }

    public SimpleRulesRuntimeEnv(SimpleRulesRuntimeEnv env) {
        super(env);
        this.methodArgumentsCacheEnable = env.isMethodArgumentsCacheEnable();
        this.cacheMode = env.getCacheMode();
        this.storage = env.storage;
    }

    @Override
    public IRuntimeEnv cloneEnvForMT() {
        return new SimpleRulesRuntimeEnv(this);
    }

    @Override
    protected IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }
    
    private volatile boolean methodArgumentsCacheEnable = false;
    private volatile CacheMode cacheMode = CacheMode.READ_ONLY;

    static final class InvocationData {
        private Object[] params;
        private int paramsHashCode;
        private boolean paramsHashCodeCalculated = false;
        private Object result;

        public InvocationData(Object[] params, Object result) {
            this.params = params;
            this.result = result;
        }

        public Object[] getParams() {
            return params;
        }

        public int getParamsHashCode() {
            if (!paramsHashCodeCalculated) {
                HashCodeBuilder builder = new HashCodeBuilder();
                builder.append(getParams());
                paramsHashCodeCalculated = true;
                paramsHashCode = builder.toHashCode();
            }
            return paramsHashCode;
        }

        public Object getResult() {
            return result;
        }
    }

    static final class Data {
        private static final int MAX_DATA_LENGTH = 1000;

        InvocationData[] invocationDatas = new InvocationData[MAX_DATA_LENGTH];
        int size = 0;

        public Object get(Object[] params) throws ResultNotFoundException {
            HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
            hashCodeBuilder.append(params);
            int hashCode = hashCodeBuilder.toHashCode();

            for (int i = 0; i < size; i++) {
                InvocationData invocationData = invocationDatas[i];
                if (hashCode == invocationData.getParamsHashCode()) {
                    EqualsBuilder equalsBuilder = new EqualsBuilder();
                    equalsBuilder.append(invocationData.getParams(), params);
                    if (equalsBuilder.isEquals()) {
                        return invocationData.getResult();
                    }
                }
            }
            throw new ResultNotFoundException();
        }

        public void add(InvocationData invocationData) {
            if (size < MAX_DATA_LENGTH) {
                invocationDatas[size] = invocationData;
                size++;
            }
        }
    }

    public boolean isMethodArgumentsCacheEnable() {
        return methodArgumentsCacheEnable;
    }

    public void changeMethodArgumentsCache(CacheMode mode) {
        this.cacheMode = mode;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setMethodArgumentsCacheEnable(boolean enable) {
        this.methodArgumentsCacheEnable = enable;
    }

    public void resetMethodArgumentsCache() {
        storage.clear();
    }

    private Storage storage = new Storage();

    public Object findInCache(Object member, Object... params) throws ResultNotFoundException {
        Data data = storage.get(member);
        if (data != null) {
            return data.get(params);
        }
        throw new ResultNotFoundException();
    }

    private static final OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();

    public void putToCache(Object member, Object[] params, Object result) {
        if (!CacheMode.READ_WRITE.equals(getCacheMode())) {
            return;
        }
        Data data = storage.get(member);
        if (data == null) {
            data = new Data();
            storage.put(member, data);
        }
        try {
            data.get(params);
        } catch (ResultNotFoundException e) {
            Object[] clonedParams = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    clonedParams[i] = cloner.deepClone(params[i]);
                }
            }
            data.add(new InvocationData(clonedParams, result));
        }
    }

    private volatile boolean ignoreRecalculate = true;
    private volatile boolean originalCalculation = true;

    public boolean isIgnoreRecalculation() {
        return ignoreRecalculate;
    }

    public void setIgnoreRecalculate(boolean ignoreRecalculate) {
        this.ignoreRecalculate = ignoreRecalculate;
    }

    public boolean isOriginalCalculation() {
        return originalCalculation;
    }

    public void setOriginalCalculation(boolean originalCalculation) {
        this.originalCalculation = originalCalculation;
    }

    List<CalculationStep> originalCalculationSteps;
    Iterator<CalculationStep> step;

    public void resetOriginalCalculationSteps() {
        this.originalCalculationSteps = null;
        initCurrentStep();
    }

    private static abstract class CalculationStep {
        private Object member;

        public CalculationStep(Object member) {
            if (member == null) {
                throw new IllegalArgumentException("Member can't be null");
            }
            this.member = member;
        }

        public Object getMember() {
            return member;
        }
    }

    private static class ForwardCalculationStep extends CalculationStep {
        public ForwardCalculationStep(Object member) {
            super(member);
        }
    }

    private static class BackwardCalculationStep extends CalculationStep {
        private Object result;

        public BackwardCalculationStep(Object member, Object result) {
            super(member);
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }

    public void registerForwardOriginalCalculationStep(Object member) {
        if (!isIgnoreRecalculation() && isOriginalCalculation()) {
            if (this.originalCalculationSteps == null){
                this.originalCalculationSteps = new LinkedList<SimpleRulesRuntimeEnv.CalculationStep>();
            }
            this.originalCalculationSteps.add(new ForwardCalculationStep(member));
        }
    }

    @SuppressWarnings("unchecked")
    public void initCurrentStep() {
        if (originalCalculationSteps != null){
            this.step = originalCalculationSteps.iterator();
        }else{
            this.step = Collections.EMPTY_LIST.iterator();
        }
    }

    public void registerBackwardOriginalCalculationStep(Object member, Object result) {
        if (!isIgnoreRecalculation() && isOriginalCalculation()) {
            if (this.originalCalculationSteps == null){
                this.originalCalculationSteps = new LinkedList<SimpleRulesRuntimeEnv.CalculationStep>();
            }
            this.originalCalculationSteps.add(new BackwardCalculationStep(member, result));
        }
    }

    public boolean registerForwardStep(Object member) {
        if (!isIgnoreRecalculation() && !isOriginalCalculation()) {
            if (step.hasNext()) {
                CalculationStep calculationStep = step.next();
                return calculationStep.member == member && calculationStep instanceof ForwardCalculationStep;
            } else {
                return false;
            }
        }
        return false;
    }

    public Object getResultFromOriginalCalculation(Object member) {
        if (!isIgnoreRecalculation() && !isOriginalCalculation()) {
            boolean flag = true;
            int level = 0;
            while (flag) {
                flag = step.hasNext();
                if (flag) {
                    CalculationStep calculationStep = step.next();
                    if (calculationStep.getMember() == member) {
                        if (calculationStep instanceof ForwardCalculationStep) {
                            level++;
                        } else {
                            if (level == 0) {
                                BackwardCalculationStep backwardCalculationStep = (BackwardCalculationStep) calculationStep;
                                return backwardCalculationStep.getResult();
                            } else {
                                level--;
                            }
                        }
                    }
                }
            }
            throw new IllegalStateException("Can't find result. Something wrong!!!");
        }
        throw new IllegalStateException("Can't use this method.");
    }

    public void registerBackwardStep(Object member) {
        if (!isIgnoreRecalculation() && !isOriginalCalculation()) {
            boolean flag = true;
            int level = 0;
            while (flag) {
                flag = step.hasNext();
                if (flag) {
                    CalculationStep calculationStep = step.next();
                    if (calculationStep.getMember() == member) {
                        if (calculationStep instanceof ForwardCalculationStep) {
                            level++;
                        } else {
                            if (level == 0) {
                                return;
                            } else {
                                level--;
                            }
                        }
                    }
                }
            }
        }
    }
    
    public IOpenClass getTopClass() {
        return topClass;
    }
    
    public void setTopClass(IOpenClass topClass) {
        this.topClass = topClass;
    }
    
    private IOpenClass topClass;
}
