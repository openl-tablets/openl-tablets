package org.openl.rules.ruleservice.client;

public interface RatingServiceIntegration<R, K, T extends RatingServiceInvoker<K>> {
	public R invoke(T ratingServiceInvoker, Object... args) throws Exception;
}
