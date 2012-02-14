package org.openl.rules.ruleservice.client;

public interface RatingServiceIntegration<R> {
	public R invoke(Object... args) throws Exception;
}
