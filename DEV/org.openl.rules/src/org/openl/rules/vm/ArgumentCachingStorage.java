package org.openl.rules.vm;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.rules.table.OpenLArgumentsCloner;

public class ArgumentCachingStorage {

    private List<CalculationStep> originalCalculationSteps;
    private Iterator<CalculationStep> step;

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

    private final OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();

    public void putToCache(Object member, Object[] params, Object result) {
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

    public void resetOriginalCalculationSteps() {
        this.originalCalculationSteps = null;
        initCurrentStep();
    }

    private abstract static class CalculationStep {
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

    @SuppressWarnings("unchecked")
    public void initCurrentStep() {
        if (originalCalculationSteps != null) {
            this.step = originalCalculationSteps.iterator();
        } else {
            this.step = Collections.EMPTY_LIST.iterator();
        }
    }

    public Object getValueFromOriginalCalculation(Object member) {
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

    public void makeForwardStepForOriginalCalculation(Object member) {
        if (this.originalCalculationSteps == null) {
            this.originalCalculationSteps = new LinkedList<>();
        }
        this.originalCalculationSteps.add(new ForwardCalculationStep(member));
    }

    public boolean makeForwardStep(Object member) {
        if (step.hasNext()) {
            CalculationStep calculationStep = step.next();
            return calculationStep.member == member && calculationStep instanceof ForwardCalculationStep;
        } else {
            return false;
        }
    }

    public void makeBackwardStepForOriginalCalculation(Object member, Object result) {
        if (this.originalCalculationSteps == null) {
            this.originalCalculationSteps = new LinkedList<>();
        }
        this.originalCalculationSteps.add(new BackwardCalculationStep(member, result));
    }

    public void makeBackwardStep(Object member) {
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
                paramsHashCodeCalculated = true;
                paramsHashCode = Arrays.deepHashCode(getParams());
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
            int hashCode = Arrays.deepHashCode(params);

            for (int i = 0; i < size; i++) {
                InvocationData invocationData = invocationDatas[i];
                if (hashCode == invocationData.getParamsHashCode()) {
                    if (Arrays.deepEquals(invocationData.getParams(), params)) {
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

    static class Storage {
        private final Map<Object, Data> storage = new WeakHashMap<>();

        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        private final Lock readLock = readWriteLock.readLock();

        private final Lock writeLock = readWriteLock.writeLock();

        public Data get(Object key) {
            readLock.lock();
            try {
                return storage.get(key);
            } finally {
                readLock.unlock();
            }
        }

        public Data put(Object key, Data data) {
            writeLock.lock();
            try {
                return storage.put(key, data);
            } finally {
                writeLock.unlock();
            }
        }

        public void clear() {
            writeLock.lock();
            try {
                storage.clear();
            } finally {
                writeLock.unlock();
            }
        }

    }
}
