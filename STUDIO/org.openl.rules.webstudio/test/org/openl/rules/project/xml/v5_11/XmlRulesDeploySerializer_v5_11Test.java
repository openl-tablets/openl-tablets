package org.openl.rules.project.xml.v5_11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.project.xml.BaseRulesDeploySerializerTest.generateRulesDeployForTest;


import java.io.FileInputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;
import org.openl.rules.project.model.v5_11.converter.RulesDeployVersionConverter_v5_11;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

public class XmlRulesDeploySerializer_v5_11Test {

    @Test
    public void testReadRulesDeploy() throws Exception {
        FileInputStream fis = new FileInputStream("test-resources/org.openl.rules.project.xml/rules-deploy.xml");
        BaseRulesDeploySerializer<RulesDeploy_v5_11> serializer =
                new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_11(), RulesDeploy_v5_11.class);
        RulesDeploy rulesDeploy = serializer.deserialize(fis);

        assertNotNull(rulesDeploy);
        assertEquals(Boolean.FALSE, rulesDeploy.isProvideRuntimeContext());
        assertEquals(Boolean.TRUE, rulesDeploy.isProvideVariations());
        assertEquals("rulesDeployName", rulesDeploy.getServiceName());
        assertNull(rulesDeploy.getPublishers());
        assertEquals(String.class.getName(), rulesDeploy.getInterceptingTemplateClassName());
        assertNull(rulesDeploy.getAnnotationTemplateClassName());
        assertNull(rulesDeploy.getRmiServiceClass());
        assertEquals("someURL", rulesDeploy.getUrl());
        assertNull(rulesDeploy.getRmiName());
        assertNull(rulesDeploy.getVersion());
        assertNull(rulesDeploy.getGroups());
        assertEquals(String.class.getName(), rulesDeploy.getServiceClass());
        assertNull(rulesDeploy.getLazyModulesForCompilationPatterns());
        assertEquals(Map.of("rootClassNamesBinding", "com.chartis.premier.rating.result.ChartisCompoundStep,com.chartis.premier.rating.result.ChartisSimpleStep",
                        "some.other.key", "some.other.value"),
                rulesDeploy.getConfiguration());
    }

    @Test
    public void testWriteRulesDeploy() throws Exception {
        RulesDeploy rulesDeploy = generateRulesDeployForTest();
        BaseRulesDeploySerializer<RulesDeploy_v5_11> serializer =
                new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_11(), RulesDeploy_v5_11.class);
        String value = serializer.serialize(rulesDeploy);
        assertEquals(EXPECTED_VALUE, value);
    }

    private static final String EXPECTED_VALUE = "<rules-deploy>\n" +
            "    <isProvideRuntimeContext>false</isProvideRuntimeContext>\n" +
            "    <isProvideVariations>true</isProvideVariations>\n" +
            "    <serviceName>rulesDeployName</serviceName>\n" +
            "    <interceptingTemplateClassName>java.lang.String</interceptingTemplateClassName>\n" +
            "    <serviceClass>java.lang.String</serviceClass>\n" +
            "    <url>someURL</url>\n" +
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
