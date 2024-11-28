package org.openl.rules.project.xml;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.WildcardPattern;

public class BaseRulesDeploySerializerTest {

    public static RulesDeploy generateRulesDeployForTest() {
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("rulesDeployName");
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setProvideVariations(true);
        rulesDeploy.setLazyModulesForCompilationPatterns(
                new WildcardPattern[]{new WildcardPattern("some1*"),
                        new WildcardPattern("some2*")});
        rulesDeploy.setInterceptingTemplateClassName(String.class.getName());
        rulesDeploy.setAnnotationTemplateClassName(String.class.getName());
        rulesDeploy.setServiceClass(String.class.getName());
        rulesDeploy.setUrl("someURL");
        rulesDeploy.setVersion("v1");
        rulesDeploy.setPublishers(new RulesDeploy.PublisherType[]{RulesDeploy.PublisherType.WEBSERVICE});
        rulesDeploy.setGroups("group1,group2");
        rulesDeploy.setRmiName("rmiName");
        rulesDeploy.setRmiServiceClass(String.class.getName());
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("key", "value");
        configuration.put("key2", "value2");
        rulesDeploy.setConfiguration(configuration);
        return rulesDeploy;
    }
}