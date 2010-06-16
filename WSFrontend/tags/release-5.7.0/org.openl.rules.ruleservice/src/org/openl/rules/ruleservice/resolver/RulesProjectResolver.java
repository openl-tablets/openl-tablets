package org.openl.rules.ruleservice.resolver;

import java.io.File;
import java.util.List;

import org.openl.rules.ruleservice.loader.DeploymentInfo;

/**
 * Finds and understands OpenL rules projects configurations. Collects and
 * returns info about rules projects.
 */
public interface RulesProjectResolver {

    /**
     * Returns information about all rules projects in folder. Each first-level
     * subfolder is assessed if it's rules project and information about it is
     * collected.
     * 
     * @param workspaceFolder folder with rules projects in it
     * @return list of found rules projects
     */
    List<RulesProjectInfo> resolve(File workspaceFolder);

    RulesProjectInfo resolveRulesProject(File rulesProjectFolder);
}