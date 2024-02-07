package org.openl.rules.ruleservice.loader;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.DefaultConversionService;

import org.openl.util.StringUtils;

public enum DeployStrategy {

    /**
     * Always deploys project to the production repository
     */
    ALWAYS,
    /**
     * Deploys is disabled
     */
    NEVER,
    /**
     * Deploys project to the production repository only if it is absent there
     */
    IF_ABSENT;

    public static DeployStrategy fromString(String source) {
        source = StringUtils.trimToNull(source);
        if (source == null) {
            return DeployStrategy.NEVER;
        }
        var conversionService = DefaultConversionService.getSharedInstance();
        try {
            var booleanValue = conversionService.convert(source, Boolean.class);
            if (booleanValue != null) {
                return booleanValue ? DeployStrategy.IF_ABSENT : DeployStrategy.NEVER;
            }
        } catch (ConversionFailedException ignore) {
            // Do nothing
        }
        return conversionService.convert(source, DeployStrategy.class);
    }

}
