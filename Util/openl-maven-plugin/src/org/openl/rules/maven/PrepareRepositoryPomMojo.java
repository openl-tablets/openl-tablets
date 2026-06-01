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
 * Re-materialises a pom-less OpenL project's install/deploy pom at {@code target/openl-pom.xml} — the raw
 * model stripped of {@code <parent>} and the {@code <build>} bootstrap stub — so the install/deploy plugins
 * read a valid flat pom. The project root is never touched.
 * <p>
 * Bound to {@code validate} so the file exists early: the participant writes it eagerly at session start, but
 * {@code clean} may wipe {@code target/} first, and a sibling depending on this pom-less zip must resolve its
 * pom. Does not call {@code setFile} — the participant already retargeted {@code getFile()} at the generated
 * file, and re-setting would re-derive {@code basedir} at {@code target/} and break later phases.
 * <p>
 * Has no effect on classic OpenL projects, whose {@code getFile()} already points at a real {@code pom.xml}.
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
            return; // not a pom-less project (the participant retargets getFile() at openl-pom.xml)
        }
        // Re-create the install pom in case clean wiped target/ since the participant's session-start write.
        // Uses the raw model so super-pom additions (<repositories>/<reporting>/...) don't leak into the pom.
        try {
            var pomFile = OpenLPackagings.materialiseInstallPom(project.getOriginalModel(),
                    buildDirectory.toPath());
            getLog().info("Generated repository pom: " + pomFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate repository pom under " + buildDirectory, e);
        }
    }
}
