package org.openl.rules.webstudio.dependencies;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;

/**
 * Reads the POMs a {@code pom.xml} refers to — its parents and the BOMs it imports — from the Maven repository.
 *
 * <p>A version range, as a parent may name, is read as its highest version the repository has. The repositories a
 * POM declares are not read: OpenL Studio reads the one Maven repository it is set to.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
final class PomModelResolver implements ModelResolver {

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    @Override
    public ModelSource2 resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        var pom = new DefaultArtifact(groupId, artifactId, "", MavenDependencyResolver.POM, version);
        var request = new ArtifactRequest(pom, repositories, null);
        try {
            return new FileModelSource(system.resolveArtifact(session, request).getArtifact().getPath().toFile());
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }
    }

    @Override
    public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
        parent.setVersion(versionOf(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()));
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
        dependency.setVersion(versionOf(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
        // Only the Maven repository OpenL Studio is set to is read
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        // Only the Maven repository OpenL Studio is set to is read
    }

    @Override
    public ModelResolver newCopy() {
        // Nothing is kept from one reading to another
        return this;
    }

    /** The version, or the highest one of a range the repository has. */
    private String versionOf(String groupId, String artifactId, String version) throws UnresolvableModelException {
        if (!version.startsWith("[") && !version.startsWith("(")) {
            return version;
        }
        var range = new DefaultArtifact(groupId, artifactId, "", MavenDependencyResolver.POM, version);
        var request = new VersionRangeRequest(range, repositories, null);
        try {
            var highest = system.resolveVersionRange(session, request).getHighestVersion();
            if (highest == null) {
                throw new UnresolvableModelException("No version of the range " + version + " is found.",
                        groupId,
                        artifactId,
                        version);
            }
            return highest.toString();
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }
    }
}
