package org.openl.rules.project.model.v5_15.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_15.RulesDeploy_v5_15;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_15> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_15 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        if (oldVersion.getLazyModulesForCompilationPatterns() != null) {
            List<RulesDeploy.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                Arrays.asList(oldVersion.getLazyModulesForCompilationPatterns()),
                e -> e == null ? null : new RulesDeploy.WildcardPattern(e.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                .toArray(new RulesDeploy.WildcardPattern[lazyModulesForCompilationPatterns.size()]));
        }

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(oldVersion.isProvideVariations());

        if (oldVersion.getPublishers() != null) {
            List<RulesDeploy.PublisherType> publishers = CollectionUtils.map(Arrays.asList(oldVersion.getPublishers()),
                version -> {
                    if (version == null) {
                        return null;
                    }

                    switch (version) {
                        case WEBSERVICE:
                            return RulesDeploy.PublisherType.WEBSERVICE;
                        case RESTFUL:
                            return RulesDeploy.PublisherType.RESTFUL;
                        default:
                            throw new IllegalArgumentException();
                    }
                });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy.PublisherType[publishers.size()]));
        }

        rulesDeploy.setServiceClass(oldVersion.getServiceClass());
        rulesDeploy.setServiceName(oldVersion.getServiceName());
        rulesDeploy.setUrl(oldVersion.getUrl());

        return rulesDeploy;
    }

    @Override
    public RulesDeploy_v5_15 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_15 rulesDeploy = new RulesDeploy_v5_15();

        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());

        if (currentVersion.getLazyModulesForCompilationPatterns() != null) {
            List<RulesDeploy_v5_15.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                Arrays.asList(currentVersion.getLazyModulesForCompilationPatterns()),
                version -> version == null ? null : new RulesDeploy_v5_15.WildcardPattern(version.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                .toArray(new RulesDeploy_v5_15.WildcardPattern[lazyModulesForCompilationPatterns.size()]));
        }

        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());

        if (currentVersion.getPublishers() != null) {
            List<RulesDeploy_v5_15.PublisherType> publishers = CollectionUtils
                .map(Arrays.asList(currentVersion.getPublishers()), oldVersion -> {
                    if (oldVersion == null) {
                        return null;
                    }

                    switch (oldVersion) {
                        case WEBSERVICE:
                            return RulesDeploy_v5_15.PublisherType.WEBSERVICE;
                        case RESTFUL:
                            return RulesDeploy_v5_15.PublisherType.RESTFUL;
                        case RMI:
                            throw new UnsupportedOperationException("RMI publisher is not supported in old version.");
                        case KAFKA:
                            throw new UnsupportedOperationException("KAFKA publisher is not supported in old version.");
                        default:
                            throw new IllegalArgumentException();
                    }
                });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy_v5_15.PublisherType[publishers.size()]));
        }

        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());

        return rulesDeploy;
    }
}
