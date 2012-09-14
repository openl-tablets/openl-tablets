package org.openl.rules.project;

import java.io.InputStream;

import org.openl.rules.project.model.RulesDeploy;

public interface IRulesDeploySerializer {
    RulesDeploy deserialize(InputStream source);

    String serialize(RulesDeploy source);
}
