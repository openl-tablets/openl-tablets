package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;

class XmlRulesDeployGuiWrapperSerializerTest {

    @Test
    void testSerialize() throws Exception {
        RulesDeploy rulesDeploy = new RulesDeploy();
        rulesDeploy.setProvideRuntimeContext(false);
        rulesDeploy.setConfiguration(Map.of("someKey", "someValue"));

        RulesDeployGuiWrapper wrapper = new RulesDeployGuiWrapper(rulesDeploy);
        String config = """
                    <configuration>
                    <entry>
                      <string>key2</string>
                      <rules-deploy>
                        <serviceClass>s</serviceClass>
                      </rules-deploy>
                    </entry>
                    <entry>
                      <string>key1</string>
                      <string>value2</string>
                    </entry>
                  </configuration>
                """;
        wrapper.setConfiguration(config);

        String serialized = new XmlRulesDeployGuiWrapperSerializer().serialize(wrapper
        );

        String expected = """
                <rules-deploy>
                    <isProvideRuntimeContext>false</isProvideRuntimeContext>
                    <configuration>
                    <entry>
                      <string>key2</string>
                      <rules-deploy>
                        <serviceClass>s</serviceClass>
                      </rules-deploy>
                    </entry>
                    <entry>
                      <string>key1</string>
                      <string>value2</string>
                    </entry>
                  </configuration>

                </rules-deploy>
                """;
        assertEquals(expected, serialized);
    }

    @Test
    void testDeserialize() {
        String value = """
                <rules-deploy>
                  <isProvideRuntimeContext>false</isProvideRuntimeContext>
                  <isProvideVariations>false</isProvideVariations>
                  <serviceName>a</serviceName>
                  <serviceClass>b</serviceClass>
                  <url>c</url>
                    <configuration>
                    <entry>
                      <string>key2</string>
                      <rules-deploy>
                        <serviceClass>s</serviceClass>
                      </rules-deploy>
                    </entry>
                    <entry>
                      <string>key1</string>
                      <string>value2</string>
                    </entry>
                  </configuration>
                </rules-deploy>""";
        RulesDeployGuiWrapper wrapper = new XmlRulesDeployGuiWrapperSerializer().deserialize(value
        );
        String expected = """
                <configuration>
                    <entry>
                      <string>key2</string>
                      <rules-deploy>
                        <serviceClass>s</serviceClass>
                      </rules-deploy>
                    </entry>
                    <entry>
                      <string>key1</string>
                      <string>value2</string>
                    </entry>
                  </configuration>""";

        assertEquals(expected, wrapper.getConfiguration());
    }
}
