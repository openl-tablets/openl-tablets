package org.openl.rules.liveexcel;

/**
 * Context for evaluations in LiveExcel.
 * 
 * @author PUdalau
 */
public class EvaluationContext {
    private DataPool dataPool;
    private ServiceModelAPI serviceModelAPI;

    /**
     * Creates EvaluationContext.
     * 
     * @param dataPool DataPool for context.
     * @param serviceModelAPI ServiceModelAPI for context.
     */
    public EvaluationContext(DataPool dataPool, ServiceModelAPI serviceModelAPI) {
        this.dataPool = dataPool;
        this.serviceModelAPI = serviceModelAPI;
    }

    /**
     * @return DataPool of current evaluation.
     */
    public DataPool getDataPool() {
        return dataPool;
    }

    /**
     * @return ServiceModelAPI of current evaluation.
     */
    public ServiceModelAPI getServiceModelAPI() {
        return serviceModelAPI;
    }

    /**
     * Cleans up context.
     */
    public void resetContext() {
        dataPool.removeAll();
    }
}
