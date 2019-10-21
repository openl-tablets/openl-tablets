package org.openl.rules.project.model.v5_14.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_14.RulesDeploy_v5_14;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_14> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_14 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

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
    public RulesDeploy_v5_14 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_14 rulesDeploy = new RulesDeploy_v5_14();

        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());
        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());

        if (currentVersion.getPublishers() != null) {
            List<RulesDeploy_v5_14.PublisherType> publishers = CollectionUtils
                .map(Arrays.asList(currentVersion.getPublishers()), oldVersion -> {
                    if (oldVersion == null) {
                        return null;
                    }

                    switch (oldVersion) {
                        case WEBSERVICE:
                            return RulesDeploy_v5_14.PublisherType.WEBSERVICE;
                        case RESTFUL:
                            return RulesDeploy_v5_14.PublisherType.RESTFUL;
                        case RMI:
                            throw new UnsupportedOperationException("RMI publisher is not supported in old version.");
                        case KAFKA:
                            throw new UnsupportedOperationException("KAFKA publisher is not supported in old version.");
                        default:
                            throw new IllegalArgumentException();
                    }
                });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy_v5_14.PublisherType[publishers.size()]));
        }

        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());

        return rulesDeploy;
    }
}
