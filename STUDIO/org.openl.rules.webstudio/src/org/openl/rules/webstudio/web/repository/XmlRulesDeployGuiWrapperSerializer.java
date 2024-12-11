package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.xml.bind.JAXBException;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

public class XmlRulesDeployGuiWrapperSerializer {
    private static final Pattern CONFIGURATION_PATTERN = Pattern.compile("<configuration>.*</configuration>",
            Pattern.DOTALL);
    private static final Pattern ENCLOSING_CONFIG_PATTERN = Pattern
            .compile("^\\s*<configuration>.*</configuration>\\s*$", Pattern.DOTALL);
    private static final XmlRulesDeploySerializer RULES_DEPLOY_XML_SERIALIZER = new XmlRulesDeploySerializer();

    public String serialize(final RulesDeployGuiWrapper wrapper) throws JAXBException, IOException {
        var rulesDeploy = RULES_DEPLOY_XML_SERIALIZER.serialize(wrapper.getRulesDeploy());
        rulesDeploy = rulesDeploy.replaceAll("<configuration>[\\s\\S]*?</configuration>", "")
                .replaceAll("(?m)^[ \t]*\r?\n", "");

        String configuration = "";
        if (StringUtils.isNotBlank(wrapper.getConfiguration())) {
            configuration = wrapper.getConfiguration();
            boolean enclosingConfigTags = ENCLOSING_CONFIG_PATTERN.matcher(configuration).matches();
            if (!enclosingConfigTags) {
                configuration = "<configuration>\n" + configuration + "\n</configuration>";
            }
            configuration += "\n";
        }

        int pasteIndex = rulesDeploy.lastIndexOf("</" + XmlRulesDeploySerializer.RULES_DEPLOY_DESCRIPTOR_TAG + ">");
        rulesDeploy = rulesDeploy.substring(0, pasteIndex) + configuration + rulesDeploy.substring(pasteIndex);
        return rulesDeploy;
    }

    public RulesDeployGuiWrapper deserialize(String source) throws JAXBException {
        Matcher matcher = CONFIGURATION_PATTERN.matcher(source);
        String configuration = null;

        if (matcher.find()) {
            configuration = matcher.group();
            source = matcher.replaceFirst("");
        }

        RulesDeploy rulesDeploy = RULES_DEPLOY_XML_SERIALIZER.deserialize(IOUtils.toInputStream(source));
        RulesDeployGuiWrapper result = new RulesDeployGuiWrapper(rulesDeploy);
        result.setConfiguration(configuration);
        return result;
    }
}
