package org.openl.rules.maven;

import org.openl.conf.ant.JavaInterfaceAntTask;

public class GenerateInterface extends JavaInterfaceAntTask {

    public GenerateInterface() {
        // TODO setGoal() should be refactored: now it's usage is inconvenient
        // and unclear.
        // For interface generation only "generate datatypes" goal is needed
        // Can be overridden in maven configuration
        setGoal(GOAL_GENERATE_DATATYPES);
    }
}
