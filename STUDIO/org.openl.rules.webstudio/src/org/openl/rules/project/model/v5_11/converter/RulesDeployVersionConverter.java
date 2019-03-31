package org.openl.rules.project.model.v5_11.converter;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;

public class RulesDeployVersionConverter implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_11> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_11 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(oldVersion.isProvideVariations());

        rulesDeploy.setServiceClass(oldVersion.getServiceClass());
        rulesDeploy.setServiceName(oldVersion.getServiceName());
        rulesDeploy.setUrl(oldVersion.getUrl());

        return rulesDeploy;
    }

    @Override
    public RulesDeploy_v5_11 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_11 rulesDeploy = new RulesDeploy_v5_11();

        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());
        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());
        rulesDeploy.setProvideVariations(currentVersion.isProvideVariations());
        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());

        return rulesDeploy;
    }
}
