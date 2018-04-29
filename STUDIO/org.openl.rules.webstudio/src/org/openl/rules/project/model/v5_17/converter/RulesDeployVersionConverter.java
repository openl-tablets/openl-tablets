package org.openl.rules.project.model.v5_17.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_17.RulesDeploy_v5_17;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_17> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_17 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setAnnotationTemplateClassName(oldVersion.getAnnotationTemplateClassName());
        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setGroups(oldVersion.getGroups());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        if (oldVersion.getLazyModulesForCompilationPatterns() != null) {
            List<RulesDeploy.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(Arrays.asList(
                    oldVersion.getLazyModulesForCompilationPatterns()),
                    new CollectionUtils.Mapper<RulesDeploy_v5_17.WildcardPattern, RulesDeploy.WildcardPattern>() {
                        @Override
                        public RulesDeploy.WildcardPattern map(RulesDeploy_v5_17.WildcardPattern oldVersion) {
                            return oldVersion == null ? null : new RulesDeploy.WildcardPattern(oldVersion.getValue());

                        }
                    }
            );
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns.toArray(new RulesDeploy.WildcardPattern[lazyModulesForCompilationPatterns.size()]));
        }

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(oldVersion.isProvideVariations());

        if (oldVersion.getPublishers() != null) {
            List<RulesDeploy.PublisherType> publishers = CollectionUtils.map(Arrays.asList(oldVersion.getPublishers()),
                    new CollectionUtils.Mapper<RulesDeploy_v5_17.PublisherType, RulesDeploy.PublisherType>() {
                        @Override
                        public RulesDeploy.PublisherType map(RulesDeploy_v5_17.PublisherType oldVersion) {
                            if (oldVersion == null) {
                                return null;
                            }

                            switch (oldVersion) {
                                case WEBSERVICE:
                                    return RulesDeploy.PublisherType.WEBSERVICE;
                                case RESTFUL:
                                    return RulesDeploy.PublisherType.RESTFUL;
                                case RMI:
                                    return RulesDeploy.PublisherType.RMI;
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                    }
            );
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy.PublisherType[publishers.size()]));
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
            List<RulesDeploy_v5_17.WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                    Arrays.asList(currentVersion.getLazyModulesForCompilationPatterns()),
                    new CollectionUtils.Mapper<RulesDeploy.WildcardPattern, RulesDeploy_v5_17.WildcardPattern>() {
                        @Override
                        public RulesDeploy_v5_17.WildcardPattern map(RulesDeploy.WildcardPattern oldVersion) {
                            return oldVersion == null ?
                                   null :
                                   new RulesDeploy_v5_17.WildcardPattern(oldVersion.getValue());
                        }
                    }
            );
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns.toArray(new RulesDeploy_v5_17.WildcardPattern[lazyModulesForCompilationPatterns.size()]));
        }
        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());

        if (currentVersion.getPublishers() != null) {
            List<RulesDeploy_v5_17.PublisherType> publishers = CollectionUtils.map(Arrays.asList(currentVersion.getPublishers()),
                    new CollectionUtils.Mapper<RulesDeploy.PublisherType, RulesDeploy_v5_17.PublisherType>() {
                        @Override
                        public RulesDeploy_v5_17.PublisherType map(RulesDeploy.PublisherType oldVersion) {
                            if (oldVersion == null) {
                                return null;
                            }

                            switch (oldVersion) {
                                case WEBSERVICE:
                                    return RulesDeploy_v5_17.PublisherType.WEBSERVICE;
                                case RESTFUL:
                                    return RulesDeploy_v5_17.PublisherType.RESTFUL;
                                case RMI:
                                    return RulesDeploy_v5_17.PublisherType.RMI;
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                    }
            );
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy_v5_17.PublisherType[publishers.size()]));
        }

        rulesDeploy.setRmiServiceClass(currentVersion.getRmiServiceClass());
        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());
        rulesDeploy.setVersion(currentVersion.getVersion());

        return rulesDeploy;
    }
}
