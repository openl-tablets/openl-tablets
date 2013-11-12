package org.openl.rules.maven.plugin.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal that copies the project dependencies from the repository to a defined
 * location. Unlike maven-dependency-plugin:copy-dependencies goal, allows to
 * set regex exclusion patterns and list of transitively excluded artifact
 * items.
 * 
 * @goal copy-lib
 * 
 * @requiresDependencyResolution runtime
 * 
 * @author Andrey Naumenko
 * 
 * @todo enable full processing of transitivelyExcludedArtifactItems list, not
 *       only first item
 */
public class CopyLibMojo extends AbstractMojo {
    /**
     * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
     * artifactId, version, type, classifier, location, destFile, markerFile and
     * overwrite.) See "Usage" and "Javadoc" for details.
     * 
     * @parameter
     * @required
     */
    private ArrayList<ArtifactItem> artifactItems;

    /**
     * List of transitively excluded dependencies.
     * 
     * @parameter expression="${dependencyExclusion}"
     */
    private ArrayList<ArtifactItem> transitivelyExcludedArtifactItems = new ArrayList<ArtifactItem>();

    /**
     * Comma separated list of regex patterns of artifact names too exclude.
     * 
     * @optional
     * @parameter expression="${excludeArtifactIds}" default-value=""
     */
    private String excludeArtifactIds;

    /**
     * Output directory.
     * 
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/lib"
     * @optional
     */
    private File outputDirectory;

    /**
     * Artifact factory, needed to download source jars.
     * 
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected java.util.List remoteRepos;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression=
     *            "${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    protected org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression=
     *            "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @component 
     *            role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     *            hint="maven"
     * @required
     * @readonly
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    private void resolveTransitiveDependencies(ArtifactResolver artifactResolver, String scope,
            ArtifactFactory artifactFactory, MavenProject project, boolean isAggregator)
            throws ArtifactResolutionException, ArtifactNotFoundException, InvalidDependencyVersionException {
        ArtifactFilter filter = new ScopeArtifactFilter(scope);

        // TODO: such a call in MavenMetadataSource too - packaging not really
        // the intention of type
        Artifact artifact = artifactFactory.createBuildArtifact(project.getGroupId(), project.getArtifactId(), project
                .getVersion(), project.getPackaging());

        // TODO: we don't need to resolve over and over again, as long as we are
        // sure that the parameters are the same
        // check this with yourkit as a hot spot.
        // Don't recreate if already created - for effeciency, and because
        // clover plugin adds to it
        if (project.getDependencyArtifacts() == null) {
            project.setDependencyArtifacts(project.createArtifacts(artifactFactory, null, null));
        }

        Set resolvedArtifacts;
        try {
            ArtifactResolutionResult result = artifactResolver.resolveTransitively(project.getDependencyArtifacts(),
                    artifact, project.getManagedVersionMap(), local, project.getRemoteArtifactRepositories(),
                    artifactMetadataSource, filter);
            resolvedArtifacts = result.getArtifacts();
        } catch (MultipleArtifactsNotFoundException e) {
            /*only do this if we are an aggregating plugin: MNG-2277
            if the dependency doesn't yet exist but is in the reactor, then
            all we can do is warn and skip it. A better fix can be inserted into 2.1*/
            throw e;
        }
        project.setArtifacts(resolvedArtifacts);
    }

    public void execute() throws MojoExecutionException {
        ArtifactItem excludedArtifactItem = artifactItems.iterator().next();
        ArtifactHandler artifactHandler = new DefaultArtifactHandler();
        Artifact excludedArtifact = new DefaultArtifact(excludedArtifactItem.getGroupId(), excludedArtifactItem
                .getArtifactId(), VersionRange.createFromVersion(excludedArtifactItem.getVersion()), "", "jar", "",
                artifactHandler);

        MavenProject project = null;
        try {
            project = mavenProjectBuilder.buildFromRepository(excludedArtifact, remoteRepos, local);
        } catch (ProjectBuildingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Collection<MavenProject> projects;
        projects = Collections.singleton(project);

        for (MavenProject p : projects) {
            try {
                resolveTransitiveDependencies(artifactResolver, Artifact.SCOPE_RUNTIME, artifactFactory, p, false);
            } catch (ArtifactResolutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ArtifactNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidDependencyVersionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!transitivelyExcludedArtifactItems.isEmpty()) {
            excludedArtifactItem = transitivelyExcludedArtifactItems.iterator().next();
            excludedArtifact = new DefaultArtifact(excludedArtifactItem.getGroupId(), excludedArtifactItem
                    .getArtifactId(), VersionRange.createFromVersion(excludedArtifactItem.getVersion()), "", "jar", "",
                    new DefaultArtifactHandler());

            MavenProject excludedProject = null;
            try {
                excludedProject = mavenProjectBuilder.buildFromRepository(excludedArtifact, remoteRepos, local);
            } catch (ProjectBuildingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            projects = Collections.singleton(excludedProject);

            for (MavenProject p : projects) {
                try {
                    resolveTransitiveDependencies(artifactResolver, Artifact.SCOPE_RUNTIME, artifactFactory, p, false);
                } catch (ArtifactResolutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ArtifactNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvalidDependencyVersionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            getLog().info("transitively excluded artifacts:");
            getLog().info(excludedProject.getArtifacts().toString());
            project.getArtifacts().removeAll(excludedProject.getArtifacts());
            getLog().info("after removing transitively excluded artifacts:");
            getLog().info(project.getArtifacts().toString());
        }

        String[] excludeIds = excludeArtifactIds.split(",");

        for (Artifact a : (Set<Artifact>) project.getArtifacts()) {
            try {
                boolean ok = true;
                for (String excludeId : excludeIds) {
                    if (a.getArtifactId().matches(excludeId)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    FileUtils.copyFileToDirectory(a.getFile(), outputDirectory);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
