package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Materialises the in-memory {@link MavenProject#getOriginalModel()} of a pom-less OpenL project as
 * a standard Maven XML pom inside {@code target/openl-pom.xml}, strips the {@code <build>}
 * bootstrap stub the participant injected for {@code ModelBuilder} validation, and points
 * {@link MavenProject#setFile} at the generated file so the standard install/deploy plugins read a
 * valid XML pom. The project root is never touched — only files in {@code target/} are produced.
 * <p>
 * Bound first in the {@code install} lifecycle phase by the {@code openl} packaging mapping in
 * {@code components.xml}, so it also runs on {@code mvn deploy} (which includes install).
 * <p>
 * Has no effect on classic OpenL projects whose {@code project.getFile()} already points at a real
 * {@code pom.xml}.
 *
 * @author Yury Molchan
 */
@Mojo(name = "prepare-pom", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class PrepareRepositoryPomMojo extends AbstractMojo {

    static final String GENERATED_POM_FILE_NAME = "openl-pom.xml";

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
        if (current == null || !ProjectDescriptor.FILE_NAME.equals(current.getName())) {
            // Not a pom-less OpenL project — install plugin already has a real pom to install.
            return;
        }
        // Use the ORIGINAL (raw) model, not the effective one. Super-pom additions
        // (<repositories>/<pluginRepositories>/<reporting>) only land on the effective model and
        // would otherwise leak into the installed pom.
        var minimal = project.getOriginalModel().clone();
        minimal.setBuild(null);   // strip the openl-maven-plugin bootstrap stub
        minimal.setParent(null);  // defensive — pom-less projects never declare a parent

        var pomFile = buildDirectory.toPath().resolve(GENERATED_POM_FILE_NAME);
        try {
            Files.createDirectories(pomFile.getParent());
            try (var out = Files.newBufferedWriter(pomFile)) {
                new MavenXpp3Writer().write(out, minimal);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate repository pom at " + pomFile, e);
        }
        project.setFile(pomFile.toFile());
        getLog().info("Generated repository pom: " + pomFile);
    }
}
