package org.openl.rules.project;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;

import org.openl.rules.project.model.RulesDeploy;

public interface IRulesDeploySerializer {
    RulesDeploy deserialize(InputStream source) throws JAXBException;

    String serialize(RulesDeploy source) throws IOException, JAXBException;
}
