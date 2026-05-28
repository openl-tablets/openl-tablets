package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Materialises the in-memory {@link MavenProject#getOriginalModel()} of a pom-less OpenL project as
 * a standard Maven XML pom inside {@code target/openl-pom.xml}, strips the {@code <build>}
 * bootstrap stub the participant injected for {@code ModelBuilder} validation, and points
 * {@link MavenProject#setFile} at the generated file so the standard install/deploy plugins read a
 * valid XML pom. The project root is never touched — only files in {@code target/} are produced.
 * <p>
 * Bound to the {@code verify} phase (after {@code openl:verify}) by the {@code openl} packaging mapping in
 * {@code components.xml}. {@code verify} — not {@code install} — so a sibling reactor project that depends on
 * this pom-less OpenL zip can resolve its POM during {@code mvn verify}: by then {@code getFile()} already
 * points at the on-disk {@code target/openl-pom.xml} (without this, Maven's reactor reader returns
 * {@code rules.xml} and Aether fails to parse it). The change is safe: every preceding OpenL phase that
 * needs {@code basedir = <OpenL folder>} ({@code compile}, {@code test}, {@code package}, {@code verify})
 * has already run; {@code install}/{@code deploy} consume the now-correct {@code getFile()} unchanged.
 * <p>
 * Has no effect on classic OpenL projects whose {@code project.getFile()} already points at a real
 * {@code pom.xml}.
 *
 * @author Yury Molchan
 */
@Mojo(name = "prepare-pom", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PrepareRepositoryPomMojo extends AbstractMojo {

    /** Plugin-configuration element name for the {@link #flattenGroupId} flag. */
    public static final String FLATTEN_GROUP_ID_PARAM = "flattenGroupId";

    /** {@code -D} property that overrides {@link #FLATTEN_GROUP_ID_PARAM}. */
    public static final String FLATTEN_GROUP_ID_PROPERTY = "openl.flattenGroupId";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

    /**
     * Controls how the groupId of a pom-less OpenL project is derived from its location under the
     * anchor pom. When {@code false} (default), the relative folder path is appended as dotted
     * segments to the anchor's {@code <groupId>}. When {@code true}, every pom-less project uses
     * the anchor's groupId verbatim — no path-based derivation.
     * <p>
     * <b>Read by {@code OpenLPomlessParticipant} during reactor setup, before any mojo runs.</b>
     * Declared here so {@code mvn openl:help -Ddetail=true -Dgoal=prepare-pom} documents it and
     * the CLI override {@code -Dopenl.flattenGroupId=true} works; this mojo itself does not act
     * on the value.
     *
     * @since 6.1.0
     */
    @Parameter(property = FLATTEN_GROUP_ID_PROPERTY, defaultValue = "false")
    @SuppressWarnings("unused")
    private boolean flattenGroupId;

    @Override
    public void execute() throws MojoExecutionException {
        var current = project.getFile();
        if (current == null || !OpenLPackagings.INSTALL_POM_FILE_NAME.equals(current.getName())) {
            // Not a pom-less OpenL project — the participant retargets pom-less projects' getFile() at
            // 'openl-pom.xml'. Anything else (classic pom.xml) means there's nothing to re-materialise here.
            return;
        }
        // Re-create the install pom — clean:clean wiped target/ between the participant's eager session-start
        // write and this validate-phase invocation; downstream consumers in the same reactor (a war that
        // depends on this pom-less zip) resolve project.getFile() and need it to exist on disk by now.
        // Uses the original (raw) model so super-pom additions (<repositories>/<reporting>/...) don't leak
        // into the published artefact pom. Does NOT call setFile — the participant's reflection set it once;
        // re-setting would re-derive basedir at target/ and break later phases.
        try {
            var pomFile = OpenLPackagings.materialiseInstallPom(project.getOriginalModel(),
                    buildDirectory.toPath());
            getLog().info("Generated repository pom: " + pomFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate repository pom under " + buildDirectory, e);
        }
    }
}
