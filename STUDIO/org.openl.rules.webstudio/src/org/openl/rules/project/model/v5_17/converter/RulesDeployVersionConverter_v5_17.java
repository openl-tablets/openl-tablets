package org.openl.rules.project.model.v5_17.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.WildcardPattern;
import org.openl.rules.project.model.v5_17.PublisherType_v5_17;
import org.openl.rules.project.model.v5_17.RulesDeploy_v5_17;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter_v5_17 implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_17> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_17 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setAnnotationTemplateClassName(oldVersion.getAnnotationTemplateClassName());
        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setGroups(oldVersion.getGroups());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        if (oldVersion.getLazyModulesForCompilationPatterns() != null) {
            List<WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                    Arrays.asList(oldVersion.getLazyModulesForCompilationPatterns()),
                    e -> e == null ? null : new WildcardPattern(e.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                    .toArray(new WildcardPattern[0]));
        }

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(oldVersion.isProvideVariations());

        if (oldVersion.getPublishers() != null) {
            List<RulesDeploy.PublisherType> publishers = CollectionUtils.map(Arrays.asList(oldVersion.getPublishers()),
                    e -> {
                        if (e == null) {
                            return null;
                        }

                        switch (e) {
                            case WEBSERVICE:
                                return RulesDeploy.PublisherType.WEBSERVICE;
                            case RESTFUL:
                                return RulesDeploy.PublisherType.RESTFUL;
                            case RMI:
                                return RulesDeploy.PublisherType.RMI;
                            default:
                                throw new IllegalArgumentException();
                        }
                    });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy.PublisherType[0]));
        }

        rulesDeploy.setRmiServiceClass(oldVersion.getRmiServiceClass());
        rulesDeploy.setServiceClass(oldVersion.getServiceClass());
        rulesDeploy.setServiceName(oldVersion.getServiceName());
        rulesDeploy.setUrl(oldVersion.getUrl());
        rulesDeploy.setVersion(oldVersion.getVersion());

        return rulesDeploy;
    }

    @Override
    public RulesDeploy_v5_17 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_17 rulesDeploy = new RulesDeploy_v5_17();

        rulesDeploy.setAnnotationTemplateClassName(currentVersion.getAnnotationTemplateClassName());
        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setGroups(currentVersion.getGroups());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());
        if (currentVersion.getLazyModulesForCompilationPatterns() != null) {
            List<WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                    Arrays.asList(currentVersion.getLazyModulesForCompilationPatterns()),
                    oldVersion -> oldVersion == null ? null : new WildcardPattern(oldVersion.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                    .toArray(new WildcardPattern[0]));
        }
        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());

        if (currentVersion.getPublishers() != null) {
            List<PublisherType_v5_17> publishers = CollectionUtils
                    .map(Arrays.asList(currentVersion.getPublishers()), oldVersion -> {
                        if (oldVersion == null) {
                            return null;
                        }

                        switch (oldVersion) {
                            case WEBSERVICE:
                                return PublisherType_v5_17.WEBSERVICE;
                            case RESTFUL:
                                return PublisherType_v5_17.RESTFUL;
                            case RMI:
                                return PublisherType_v5_17.RMI;
                            case KAFKA:
                                throw new UnsupportedOperationException("KAFKA publisher is not supported in old version.");
                            default:
                                throw new IllegalArgumentException();
                        }
                    });
            rulesDeploy.setPublishers(publishers.toArray(new PublisherType_v5_17[0]));
        }

        rulesDeploy.setRmiServiceClass(currentVersion.getRmiServiceClass());
        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());
        rulesDeploy.setVersion(currentVersion.getVersion());

        return rulesDeploy;
    }
}
