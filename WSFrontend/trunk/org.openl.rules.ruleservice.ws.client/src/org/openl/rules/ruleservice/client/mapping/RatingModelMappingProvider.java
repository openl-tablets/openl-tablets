package org.openl.rules.ruleservice.client.mapping;

public interface RatingModelMappingProvider {
    public Object[] mapArgsToRatingModel(Object... args) throws OpenLClientMappingException;
}
