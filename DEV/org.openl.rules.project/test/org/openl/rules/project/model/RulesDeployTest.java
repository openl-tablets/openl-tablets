package org.openl.rules.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
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
        rulesDeploy.setPublishers(new PublisherType[]{PublisherType.RESTFUL});
        rulesDeploy.setGroups("group1,group2");
        var configuration = Map.<String, Object>of("key", "value");
        rulesDeploy.setConfiguration(configuration);

        var value = new String(rulesDeploy.toBytes(), StandardCharsets.UTF_8);
        assertEquals(EXPECTED_VALUE, value);
    }

    @Test
    void testReadSkipsDeprecatedPublishers() throws Exception {
        var xml = """
                <rules-deploy>
                    <publishers>
                        <publisher>RMI</publisher>
                        <publisher>RESTFUL</publisher>
                        <publisher>WEBSERVICE</publisher>
                        <publisher>KAFKA</publisher>
                    </publishers>
                </rules-deploy>
                """;
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(rulesDeploy);
        assertNotNull(rulesDeploy.getPublishers());
        assertEquals(2, rulesDeploy.getPublishers().length);
        assertEquals(PublisherType.RESTFUL, rulesDeploy.getPublishers()[0]);
        assertEquals(PublisherType.KAFKA, rulesDeploy.getPublishers()[1]);
    }

    @Test
    void testPublisherTypeAdapterHandlesNullAndBlankInput() throws Exception {
        var adapter = new RulesDeploy.PublisherTypeXmlAdapter();
        assertNull(adapter.unmarshal(null));
        assertNull(adapter.unmarshal(""));
        assertNull(adapter.unmarshal("   "));
        assertNull(adapter.unmarshal("\n\t"));
    }

    @Test
    void testReadConfigurationSkipsMalformedEntries() throws Exception {
        var xml = """
                <rules-deploy>
                    <configuration>
                        <entry/>
                        <entry>
                            <string>orphanKey</string>
                        </entry>
                        <entry>
                            <string>k</string>
                            <string>v</string>
                        </entry>
                    </configuration>
                </rules-deploy>
                """;
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(rulesDeploy);
        assertNotNull(rulesDeploy.getConfiguration());
        assertEquals(Map.of("k", "v"), rulesDeploy.getConfiguration());
    }

    @Test
    void testReadSkipsBlankAndMissingPublisherValues() throws Exception {
        var xml = """
                <rules-deploy>
                    <publishers>
                        <publisher/>
                        <publisher></publisher>
                        <publisher>   </publisher>
                        <publisher>RESTFUL</publisher>
                    </publishers>
                </rules-deploy>
                """;
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(rulesDeploy);
        assertNotNull(rulesDeploy.getPublishers());
        assertEquals(1, rulesDeploy.getPublishers().length);
        assertEquals(PublisherType.RESTFUL, rulesDeploy.getPublishers()[0]);
    }

    @Test
    void testReadOmitsPublishersWhenOnlyDeprecated() throws Exception {
        var xml = """
                <rules-deploy>
                    <publishers>
                        <publisher>RMI</publisher>
                        <publisher>WEBSERVICE</publisher>
                    </publishers>
                </rules-deploy>
                """;
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(rulesDeploy);
        assertEquals(0, rulesDeploy.getPublishers().length);
    }

    @Test
    void testWriteOmitsBlankFields() throws Exception {
        var rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("svc");
        rulesDeploy.setUrl("   ");
        rulesDeploy.setVersion("");
        rulesDeploy.setGroups(null);
        rulesDeploy.setInterceptingTemplateClassName("\t \n");
        rulesDeploy.setAnnotationTemplateClassName("LegacyService");
        rulesDeploy.setConfiguration(java.util.Map.of());

        var value = new String(rulesDeploy.toBytes(), StandardCharsets.UTF_8);
        assertEquals("""
                <rules-deploy>
                    <serviceName>svc</serviceName>
                    <annotationTemplateClassName>LegacyService</annotationTemplateClassName>
                </rules-deploy>
                """, value);
    }

    private static final String EXPECTED_VALUE = """
            <rules-deploy>
                <isProvideRuntimeContext>false</isProvideRuntimeContext>
                <serviceName>rulesDeployName</serviceName>
                <publishers>
                    <publisher>RESTFUL</publisher>
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
