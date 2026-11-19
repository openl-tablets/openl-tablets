package org.openl.rules.webstudio.dependencies;

import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

/**
 * A dependency left out of a compilation because another project of its name is compiled.
 *
 * <p>A compilation takes one project of a name, the one its walk through the dependencies reaches first. Another
 * project of the name — another version downloaded from the Maven repository, or the archive of a project the user did
 * not open beside another one — is left out, and what depends on it is compiled against the first one.
 *
 * @param name    the name of both projects
 * @param root    the project the compilation is of
 * @param kept    how the project compiled was asked for, absent when nothing is told of it
 * @param dropped how the project left out was asked for
 * @author Yury Molchan
 */
public record DependencyConflict(String name, String root, @Nullable Request kept, Request dropped) {

    /**
     * How a project is asked for.
     *
     * @param requester     the project whose dependency asks for it, absent for the project the compilation is of
     * @param mavenArtifact the Maven coordinates the dependency names, absent when it names none
     */
    public record Request(@Nullable String requester, @Nullable String mavenArtifact) {

        private String describe(String name) {
            var what = mavenArtifact == null ? "the project '%s' of the workspace".formatted(name) : mavenArtifact;
            return requester == null ? what : "%s, which '%s' depends on,".formatted(what, requester);
        }
    }

    /** Whether the conflict is told on the project: the one the compilation is of, which names both. */
    public boolean concerns(String project) {
        return project.equals(root);
    }

    /** The warning told of the conflict. */
    public OpenLMessage message() {
        var compiled = kept == null ? "the project '%s'".formatted(name) : kept.describe(name);
        return new OpenLMessage("Two projects '%s' are required: %s is compiled, and %s is left out.".formatted(name,
                compiled,
                dropped.describe(name)), Severity.WARN);
    }
}
