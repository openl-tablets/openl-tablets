package org.openl.rules.ruleservice.client;

import org.openl.rules.ruleservice.client.mapping.RatingModelMappingProvider;
import org.openl.rules.ruleservice.client.mapping.ResultMappingProvider;

public abstract class AbstractRatingServiceIntegrationImpl<R, T, K extends RatingServiceInvoker<T>> implements
        RatingServiceIntegration<R, T, K> {

    protected RatingModelMappingProvider ratingModelMappingProvider;

    protected ResultMappingProvider<T, R> resultMappingProvider;

    public R invoke(K ratingServiceInvoker, Object... args) throws Exception {
        Object[] ratingModelArgs = ratingModelMappingProvider.mapArgsToRatingModel(args);
        T result = ratingServiceInvoker.invoke(ratingModelArgs);
        return resultMappingProvider.mapToResult(result);
    }
}
