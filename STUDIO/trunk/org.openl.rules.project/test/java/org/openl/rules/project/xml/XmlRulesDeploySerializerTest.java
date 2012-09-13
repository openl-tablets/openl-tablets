package org.openl.rules.project.xml;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy;

public class XmlRulesDeploySerializerTest {
    @Test
    public void testReadRulesDeploy() throws Exception{
        FileInputStream fis = new FileInputStream(new File("test/resources/rules-deploy/rules-deploy.xml"));
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        RulesDeploy rulesDeploy = serializer.deserialize(fis);
        
        assertNotNull(rulesDeploy);
        assertEquals("rulesDeployName", rulesDeploy.getName());
        assertEquals(Boolean.FALSE, rulesDeploy.isProvideRuntimeContext());
        assertEquals(Boolean.TRUE, rulesDeploy.isProvideVariations());
        assertEquals(String.class.getCanonicalName(), rulesDeploy.getServiceClass());
        assertEquals("someURL", rulesDeploy.getUrl());
    }
    
    @Test
    public void testWriteRulesDeploy(){
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setName("rulesDeployName");
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setProvideVariations(true);
        rulesDeploy.setServiceClass(String.class.getCanonicalName());
        rulesDeploy.setUrl("someURL");
        
        XmlRulesDeploySerializer serializer = new XmlRulesDeploySerializer();
        String value = serializer.serialize(rulesDeploy);
        String expectedValue = "<rules-deploy>" + "\n" +
                                "  <isProvideRuntimeContext>false</isProvideRuntimeContext>"+ "\n" +
                                "  <isProvideVariations>true</isProvideVariations>"+ "\n" +
                                "  <name>rulesDeployName</name>"+ "\n" +
                                "  <serviceClass>java.lang.String</serviceClass>"+ "\n" +
                                "  <url>someURL</url>"+ "\n" +
                                "</rules-deploy>";
        assertEquals(expectedValue, value);
    }
}
