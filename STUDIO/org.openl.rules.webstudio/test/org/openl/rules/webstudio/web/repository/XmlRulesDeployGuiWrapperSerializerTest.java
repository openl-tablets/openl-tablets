package org.openl.rules.webstudio.web.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.RulesDeploySerializerFactory;
import org.openl.rules.project.xml.SupportedVersion;

public class XmlRulesDeployGuiWrapperSerializerTest {

    @Test
    public void testSerialize() {
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setProvideRuntimeContext(false);

        RulesDeploySerializerFactory serializerFactory = new RulesDeploySerializerFactory("");

        RulesDeployGuiWrapper wrapper = new RulesDeployGuiWrapper(rulesDeploy, SupportedVersion.getLastVersion());
        String config = "    <configuration>\n" + "    <entry>\n" + "      <string>key2</string>\n" + "      <rules-deploy>\n" + "        <serviceClass>s</serviceClass>\n" + "      </rules-deploy>\n" + "    </entry>\n" + "    <entry>\n" + "      <string>key1</string>\n" + "      <string>value2</string>\n" + "    </entry>\n" + "  </configuration>";
        wrapper.setConfiguration(config);

        String serialized = new XmlRulesDeployGuiWrapperSerializer(serializerFactory).serialize(wrapper,
            SupportedVersion.getLastVersion());

        String expected = "<rules-deploy>\n" + "  <isProvideRuntimeContext>false</isProvideRuntimeContext>\n" + "    <configuration>\n" + "    <entry>\n" + "      <string>key2</string>\n" + "      <rules-deploy>\n" + "        <serviceClass>s</serviceClass>\n" + "      </rules-deploy>\n" + "    </entry>\n" + "    <entry>\n" + "      <string>key1</string>\n" + "      <string>value2</string>\n" + "    </entry>\n" + "  </configuration>\n" + "</rules-deploy>";
        assertEquals(expected, serialized);
    }

    @Test
    public void testDeserialize() {
        String value = "<rules-deploy>\n" + "  <isProvideRuntimeContext>false</isProvideRuntimeContext>\n" + "  <isProvideVariations>false</isProvideVariations>\n" + "  <serviceName>a</serviceName>\n" + "  <serviceClass>b</serviceClass>\n" + "  <url>c</url>\n" + "    <configuration>\n" + "    <entry>\n" + "      <string>key2</string>\n" + "      <rules-deploy>\n" + "        <serviceClass>s</serviceClass>\n" + "      </rules-deploy>\n" + "    </entry>\n" + "    <entry>\n" + "      <string>key1</string>\n" + "      <string>value2</string>\n" + "    </entry>\n" + "  </configuration>\n" + "\n" + "</rules-deploy>";
        RulesDeploySerializerFactory serializerFactory = new RulesDeploySerializerFactory("");
        RulesDeployGuiWrapper wrapper = new XmlRulesDeployGuiWrapperSerializer(serializerFactory).deserialize(value,
            SupportedVersion.getLastVersion());
        String expected = "<configuration>\n" + "    <entry>\n" + "      <string>key2</string>\n" + "      <rules-deploy>\n" + "        <serviceClass>s</serviceClass>\n" + "      </rules-deploy>\n" + "    </entry>\n" + "    <entry>\n" + "      <string>key1</string>\n" + "      <string>value2</string>\n" + "    </entry>\n" + "  </configuration>";

        assertEquals(expected, wrapper.getConfiguration());
    }
}
