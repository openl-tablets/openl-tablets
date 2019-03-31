package org.openl.rules.project.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class XmlRulesDeploySerializerTest {

    @Test
    public void testReadRulesDeploy() throws Exception {
        FileInputStream fis = new FileInputStream(new File("test-resources/rules-deploy/rules-deploy.xml"));
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        RulesDeploy rulesDeploy = serializer.deserialize(fis);

        assertNotNull(rulesDeploy);
        assertEquals("rulesDeployName", rulesDeploy.getServiceName());
        assertEquals(Boolean.FALSE, rulesDeploy.isProvideRuntimeContext());
        assertEquals(Boolean.TRUE, rulesDeploy.isProvideVariations());
        assertEquals(String.class.getCanonicalName(), rulesDeploy.getServiceClass());
        assertEquals(String.class.getCanonicalName(), rulesDeploy.getRmiServiceClass());
        assertEquals(String.class.getCanonicalName(), rulesDeploy.getInterceptingTemplateClassName());
        assertEquals(String.class.getCanonicalName(), rulesDeploy.getAnnotationTemplateClassName());
        assertNotNull(rulesDeploy.getPublishers());
        assertEquals(1, rulesDeploy.getPublishers().length);
        assertEquals(RulesDeploy.PublisherType.RESTFUL, rulesDeploy.getPublishers()[0]);
        assertEquals("someURL", rulesDeploy.getUrl());
        assertNotNull(rulesDeploy.getLazyModulesForCompilationPatterns());
        assertEquals(2, rulesDeploy.getLazyModulesForCompilationPatterns().length);
        assertEquals("some1*", rulesDeploy.getLazyModulesForCompilationPatterns()[0].getValue());
        assertEquals("some2*", rulesDeploy.getLazyModulesForCompilationPatterns()[1].getValue());
        assertEquals("v1", rulesDeploy.getVersion());
        assertEquals("group1,group2", rulesDeploy.getGroups());
    }

    @Test
    public void testWriteRulesDeploy() {
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("rulesDeployName");
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setProvideVariations(true);
        rulesDeploy.setLazyModulesForCompilationPatterns(
            new RulesDeploy.WildcardPattern[] { new RulesDeploy.WildcardPattern("some1*"),
                    new RulesDeploy.WildcardPattern("some2*") });
        rulesDeploy.setInterceptingTemplateClassName(String.class.getCanonicalName());
        rulesDeploy.setAnnotationTemplateClassName(String.class.getCanonicalName());
        rulesDeploy.setServiceClass(String.class.getCanonicalName());
        rulesDeploy.setUrl("someURL");
        rulesDeploy.setVersion("v1");
        rulesDeploy.setPublishers(new RulesDeploy.PublisherType[] { PublisherType.WEBSERVICE });
        rulesDeploy.setGroups("group1,group2");
        Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("key", "value");
        rulesDeploy.setConfiguration(configuration);
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        String value = serializer.serialize(rulesDeploy);
        String expectedValue = "<rules-deploy>" + "\n" + "  <isProvideRuntimeContext>false</isProvideRuntimeContext>" + "\n" + "  <isProvideVariations>true</isProvideVariations>" + "\n" + "  <serviceName>rulesDeployName</serviceName>" + "\n" + "  <publishers>" + "\n" + "    <publisher>WEBSERVICE</publisher>" + "\n" + "  </publishers>" + "\n" + "  <interceptingTemplateClassName>java.lang.String</interceptingTemplateClassName>" + "\n" + "  <annotationTemplateClassName>java.lang.String</annotationTemplateClassName>" + "\n" + "  <serviceClass>java.lang.String</serviceClass>" + "\n" + "  <url>someURL</url>" + "\n" + "  <version>v1</version>" + "\n" + "  <groups>group1,group2</groups>" + "\n" + "  <configuration>" + "\n" + "    <entry>" + "\n" + "      <string>key</string>" + "\n" + "      <string>value</string>" + "\n" + "    </entry>" + "\n" + "  </configuration>" + "\n" + "  <lazy-modules-for-compilation>" + "\n" + "    <module name=\"some1*\"/>" + "\n" + "    <module name=\"some2*\"/>" + "\n" + "  </lazy-modules-for-compilation>" + "\n" + "</rules-deploy>";
        assertEquals(expectedValue, value);
    }
}
