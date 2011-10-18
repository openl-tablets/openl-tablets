package org.openl.rules.ruleservice.client;

import org.openl.rules.ruleservice.client.mapping.RatingModelMappingProvider;
import org.openl.rules.ruleservice.client.mapping.ResultMappingProvider;

public abstract class AbstractRatingServiceIntegrationImpl<R, T, K extends RatingServiceInvoker<T>> implements
        RatingServiceIntegration<R> {

    protected RatingModelMappingProvider ratingModelMappingProvider;

    protected ResultMappingProvider<T, R> resultMappingProvider;

    protected K ratingServiceInvoker;

    public R invoke(Object... args) throws Exception {
        Object[] ratingModelArgs = getRatingModelMappingProvider().mapArgsToRatingModel(args);
        T result = getRatingServiceInvoker().invoke(ratingModelArgs);
        return getResultMappingProvider().mapToResult(result);
    }

    public void setRatingModelMappingProvider(RatingModelMappingProvider ratingModelMappingProvider) {
        this.ratingModelMappingProvider = ratingModelMappingProvider;
    }

    public void setResultMappingProvider(ResultMappingProvider<T, R> resultMappingProvider) {
        this.resultMappingProvider = resultMappingProvider;
    }

    public RatingModelMappingProvider getRatingModelMappingProvider() {
        return ratingModelMappingProvider;
    }

    public ResultMappingProvider<T, R> getResultMappingProvider() {
        return resultMappingProvider;
    }

    public K getRatingServiceInvoker() {
        return ratingServiceInvoker;
    }

    public void setRatingServiceInvoker(K ratingServiceInvoker) {
        this.ratingServiceInvoker = ratingServiceInvoker;
    }
}
