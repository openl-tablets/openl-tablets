package org.openl.rules.project.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class XmlRulesDeploySerializerTest {

    @Test
    public void testReadRulesDeploy() throws Exception {
        FileInputStream fis = new FileInputStream("test-resources/rules-deploy/rules-deploy.xml");
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        RulesDeploy rulesDeploy = serializer.deserialize(fis);

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
        assertEquals("rmiName", rulesDeploy.getRmiName());
        assertEquals(Map.of("rootClassNamesBinding", "com.chartis.premier.rating.result.ChartisCompoundStep,com.chartis.premier.rating.result.ChartisSimpleStep"),
                rulesDeploy.getConfiguration());
    }

    @Test
    public void testWriteRulesDeploy() throws Exception {
        RulesDeploy rulesDeploy = generateRulesDeployForTest();
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        String value = serializer.serialize(rulesDeploy);
        assertEquals(EXPECTED_VALUE, value);
    }

    public static RulesDeploy generateRulesDeployForTest() {
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("rulesDeployName");
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setInterceptingTemplateClassName(String.class.getName());
        rulesDeploy.setAnnotationTemplateClassName(String.class.getName());
        rulesDeploy.setServiceClass(String.class.getName());
        rulesDeploy.setUrl("someURL");
        rulesDeploy.setVersion("v1");
        rulesDeploy.setPublishers(new RulesDeploy.PublisherType[]{PublisherType.WEBSERVICE});
        rulesDeploy.setGroups("group1,group2");
        rulesDeploy.setRmiName("rmiName");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("key", "value");
        configuration.put("key2", "value2");
        rulesDeploy.setConfiguration(configuration);
        return rulesDeploy;
    }

    private static final String EXPECTED_VALUE = "<rules-deploy>\n" +
            "    <isProvideRuntimeContext>false</isProvideRuntimeContext>\n" +
            "    <serviceName>rulesDeployName</serviceName>\n" +
            "    <publishers>\n" +
            "        <publisher>WEBSERVICE</publisher>\n" +
            "    </publishers>\n" +
            "    <interceptingTemplateClassName>java.lang.String</interceptingTemplateClassName>\n" +
            "    <annotationTemplateClassName>java.lang.String</annotationTemplateClassName>\n" +
            "    <serviceClass>java.lang.String</serviceClass>\n" +
            "    <url>someURL</url>\n" +
            "    <rmiName>rmiName</rmiName>\n" +
            "    <version>v1</version>\n" +
            "    <groups>group1,group2</groups>\n" +
            "    <configuration>\n" +
            "        <entry>\n" +
            "            <string>key2</string>\n" +
            "            <string>value2</string>\n" +
            "        </entry>\n" +
            "        <entry>\n" +
            "            <string>key</string>\n" +
            "            <string>value</string>\n" +
            "        </entry>\n" +
            "    </configuration>\n" +
            "</rules-deploy>";
}
