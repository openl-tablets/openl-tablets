package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link MigrateMojo#pluginParameters}: the parameters of every goal of the plugin, their aliases included,
 * have an effect, except the deprecated ones the plugin ignores.
 *
 * @author Yury Molchan
 */
class MigrateMojoPluginParametersTest {

    @Test
    void collectsTheParametersOfEveryGoalWithoutTheIgnoredOnes() throws Exception {
        var plugin = new PluginDescriptor();
        plugin.addMojo(mojo("package",
                parameter("format", null),
                parameter("classpathFolder", null),
                parameter("deploymentPackage", null),
                parameter("deploymentName", null)));
        plugin.addMojo(mojo("test", parameter("skipTests", "openl.test.skip")));
        plugin.addMojo(mojo("help"));

        assertEquals(Set.of("format", "classpathFolder", "skipTests", "openl.test.skip"),
                MigrateMojo.pluginParameters(plugin));
    }

    private static MojoDescriptor mojo(String goal, Parameter... parameters) throws Exception {
        var mojo = new MojoDescriptor();
        mojo.setGoal(goal);
        for (var parameter : parameters) {
            mojo.addParameter(parameter);
        }
        return mojo;
    }

    private static Parameter parameter(String name, String alias) {
        var parameter = new Parameter();
        parameter.setName(name);
        parameter.setAlias(alias);
        return parameter;
    }
}
