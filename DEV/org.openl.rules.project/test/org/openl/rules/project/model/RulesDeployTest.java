package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy.PublisherType;

class RulesDeployTest {

    @Test
    void testReadRulesDeploy() throws Exception {
        var rulesDeploy = RulesDeploy.read(Path.of("test-resources/rules-deploy/"));

        assertNotNull(rulesDeploy);
        assertEquals("rulesDeployName", rulesDeploy.getServiceName());
        assertEquals(Boolean.FALSE, rulesDeploy.isProvideRuntimeContext());
        assertEquals(String.class.getName(), rulesDeploy.getServiceClass());
        assertEquals(String.class.getName(), rulesDeploy.getInterceptingTemplateClassName());
        assertEquals(String.class.getName(), rulesDeploy.getAnnotationTemplateClassName());
        assertNotNull(rulesDeploy.getPublishers());
        assertEquals(1, rulesDeploy.getPublishers().length);
        assertEquals(RulesDeploy.PublisherType.RESTFUL, rulesDeploy.getPublishers()[0]);
        assertEquals("someURL", rulesDeploy.getUrl());
        assertEquals("v1", rulesDeploy.getVersion());
        assertEquals("group1,group2", rulesDeploy.getGroups());
        assertEquals(Map.of("rootClassNamesBinding", "com.chartis.premier.rating.result.ChartisCompoundStep,com.chartis.premier.rating.result.ChartisSimpleStep"),
                rulesDeploy.getConfiguration());
    }

    @Test
    void testWriteRulesDeploy() throws Exception {
        var rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("rulesDeployName");
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setInterceptingTemplateClassName(String.class.getName());
        rulesDeploy.setAnnotationTemplateClassName(String.class.getName());
        rulesDeploy.setServiceClass(String.class.getName());
        rulesDeploy.setUrl("someURL");
        rulesDeploy.setVersion("v1");
        rulesDeploy.setPublishers(new PublisherType[]{PublisherType.WEBSERVICE});
        rulesDeploy.setGroups("group1,group2");
        var configuration = Map.<String, Object>of("key", "value");
        rulesDeploy.setConfiguration(configuration);

        String value;
        try (var inputStream = rulesDeploy.toInputStream()) {
            value = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(EXPECTED_VALUE, value);
    }

    private static final String EXPECTED_VALUE = """
            <rules-deploy>
                <isProvideRuntimeContext>false</isProvideRuntimeContext>
                <serviceName>rulesDeployName</serviceName>
                <publishers>
                    <publisher>WEBSERVICE</publisher>
                </publishers>
                <interceptingTemplateClassName>java.lang.String</interceptingTemplateClassName>
                <annotationTemplateClassName>java.lang.String</annotationTemplateClassName>
                <serviceClass>java.lang.String</serviceClass>
                <url>someURL</url>
                <version>v1</version>
                <groups>group1,group2</groups>
                <configuration>
                    <entry>
                        <string>key</string>
                        <string>value</string>
                    </entry>
                </configuration>
            </rules-deploy>
            """;
}
