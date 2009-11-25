package org.openl.rules.liveexcel;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.formula.WorkbookEvaluator;

/**
 * Context for evaluations in LiveExcel.
 * 
 * @author PUdalau
 */
public class EvaluationContext {
    private Map<WorkbookEvaluator, DataPool> dataPools;
    private ServiceModelAPI serviceModelAPI;

    /**
     * Creates EvaluationContext.
     * 
     * @param dataPool DataPool for context.
     * @param serviceModelAPI ServiceModelAPI for context.
     */
    public EvaluationContext(ServiceModelAPI serviceModelAPI) {
        dataPools = new HashMap<WorkbookEvaluator, DataPool>();
        this.serviceModelAPI = serviceModelAPI;
    }

    /**
     * Creates DataPool in context associated with evaluator.
     * 
     * @param evaluator Current WorkbookEvaluator.
     */
    public void createDataPool(WorkbookEvaluator evaluator) {
        dataPools.put(evaluator, new DataPool());
    }

    /**
     * @param evaluator Current WorkbookEvaluator.
     * @return DataPool associated with evaluator.
     */
    public DataPool getDataPool(WorkbookEvaluator evaluator) {
        return dataPools.get(evaluator);
    }

    /**
     * Removes DataPool associated with evaluator from context and cleares it.
     * 
     * @param evaluator Current WorkbookEvaluator.
     */
    public void removeDataPool(WorkbookEvaluator evaluator) {
        DataPool pool = dataPools.remove(evaluator);
        if(pool != null)
        	pool.removeAll();
    }

    /**
     * @return ServiceModelAPI of current evaluation.
     */
    public ServiceModelAPI getServiceModelAPI() {
        return serviceModelAPI;
    }
}
