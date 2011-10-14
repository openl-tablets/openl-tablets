package org.openl.rules.ruleservice.client;

public abstract class AbstractRatingServiceInvokerImpl<S, T> implements RatingServiceInvoker<T> {
    private S ratingService;

    private boolean needValidate;

    protected abstract boolean validateArgs(Object... args);

    protected abstract T doInvoke(Object... args) throws Exception;

    public T invoke(Object... args) throws Exception {
        if (isNeedValidate() && validateArgs(args)) {
            return doInvoke(args);
        } else {
            throw new OpenLClientException("Invoker argumens are invalid");
        }
    }

    protected S getRatingService() {
        return ratingService;
    }

    protected boolean isNeedValidate() {
        return needValidate;
    }
}
