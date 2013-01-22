package org.openl.rules.vm;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.rules.table.InputArgumentsCloner;
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

    private volatile boolean methodArgumentsCacheEnable = false;
    private volatile CacheMode cacheMode = CacheMode.READ_ONLY;

    static final class InvocationData {
        private Object[] params;
        private Object result;

        public InvocationData(Object[] params, Object result) {
            this.params = params;
            this.result = result;
        }

        public Object[] getParams() {
            return params;
        }

        public Object getResult() {
            return result;
        }
    }

    static final class Data {
        private static final int MAX_DATA_LENGTH = 1000;

        InvocationData[] invocationDatas = new InvocationData[MAX_DATA_LENGTH];
        int[] hashCodes = new int[MAX_DATA_LENGTH];
        int size = 0;

        public Object get(Object[] params) throws ResultNotFoundException {
            HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
            hashCodeBuilder.append(params);
            int hashCode = hashCodeBuilder.toHashCode();
            for (int i = 0; i < size; i++) {
                if (hashCodes[i] == hashCode) {
                    EqualsBuilder equalsBuilder = new EqualsBuilder();
                    equalsBuilder.append(invocationDatas[i].getParams(), params);
                    if (equalsBuilder.isEquals()) {
                        return invocationDatas[i].getResult();
                    }
                }
            }
            throw new ResultNotFoundException();
        }

        public void add(InvocationData invocatonData) {
            if (size < invocationDatas.length) {
                invocationDatas[size] = invocatonData;
                HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
                hashCodeBuilder.append(invocatonData.params);
                hashCodes[size] = hashCodeBuilder.toHashCode();
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

    private Map<Object, Data> storage = new WeakHashMap<Object, Data>();

    public Object findInCache(Object member, Object... params) throws ResultNotFoundException {
        Data data = storage.get(member);
        if (data != null) {
            return data.get(params);
        }
        throw new ResultNotFoundException();
    }

    private static final InputArgumentsCloner cloner = new InputArgumentsCloner();

    public void putToCache(Object member, Object[] params, Object result) {
        if (!CacheMode.READ_WRITE.equals(getCacheMode())) {
            return;
        }
        Data data = storage.get(member);
        if (data == null) {
            data = new Data();
            storage.put(member, data);
        }
        Object[] clonedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            clonedParams[i] = cloner.deepClone(params[i]);
        }
        data.add(new InvocationData(clonedParams, result));
    }

}
